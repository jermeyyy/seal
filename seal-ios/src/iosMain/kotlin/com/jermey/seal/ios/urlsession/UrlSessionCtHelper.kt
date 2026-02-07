package com.jermey.seal.ios.urlsession

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.ios.IosCertificateTransparencyVerifier
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSURLAuthenticationChallenge
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeDisposition
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecTrustRef

/**
 * Helper for integrating CT verification with URLSession's authentication challenge handling.
 *
 * Usage in a URLSessionDelegate:
 * ```kotlin
 * override fun URLSession(
 *     session: NSURLSession,
 *     didReceiveChallenge: NSURLAuthenticationChallenge,
 *     completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
 * ) {
 *     val (disposition, credential) = helper.handleServerTrustChallenge(didReceiveChallenge)
 *     completionHandler(disposition, credential)
 * }
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
public class UrlSessionCtHelper(
    private val configuration: CTConfiguration,
    private val verifier: IosCertificateTransparencyVerifier,
) {
    /**
     * Evaluate a server trust authentication challenge with CT verification.
     *
     * Call this from URLSession:didReceiveChallenge: when the protection space
     * authentication method is [NSURLAuthenticationMethodServerTrust].
     *
     * If the challenge is not a server trust challenge, returns default handling.
     *
     * @param challenge The authentication challenge from URLSession
     * @return A pair of (disposition, credential) to pass to the completion handler
     */
    public fun handleServerTrustChallenge(
        challenge: NSURLAuthenticationChallenge,
    ): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> {
        val protectionSpace = challenge.protectionSpace

        // Only handle ServerTrust challenges
        if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
            return NSURLSessionAuthChallengePerformDefaultHandling to null
        }

        val serverTrust: SecTrustRef = protectionSpace.serverTrust ?: run {
            return if (configuration.failOnError) {
                NSURLSessionAuthChallengeCancelAuthenticationChallenge to null
            } else {
                NSURLSessionAuthChallengePerformDefaultHandling to null
            }
        }

        val host = protectionSpace.host

        // Run verification (blocking — URLSession delegates are synchronous)
        val result = runBlocking {
            verifier.verify(serverTrust, host)
        }

        return when (result) {
            is VerificationResult.Success -> {
                // CT verification passed — accept the connection with the server trust credential
                NSURLSessionAuthChallengeUseCredential to NSURLCredential.credentialForTrust(serverTrust)
            }
            is VerificationResult.Failure -> {
                if (configuration.failOnError) {
                    // Fail-closed: cancel the connection
                    NSURLSessionAuthChallengeCancelAuthenticationChallenge to null
                } else {
                    // Fail-open: allow the connection despite CT failure
                    NSURLSessionAuthChallengePerformDefaultHandling to null
                }
            }
        }
    }
}
