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

@OptIn(ExperimentalForeignApi::class)
public class UrlSessionCtHelper(
    private val configuration: CTConfiguration,
    private val verifier: IosCertificateTransparencyVerifier,
) {
    public fun handleServerTrustChallenge(
        challenge: NSURLAuthenticationChallenge,
    ): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?> {
        val protectionSpace = challenge.protectionSpace

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

        val result = runBlocking {
            verifier.verify(serverTrust, host)
        }

        return when (result) {
            is VerificationResult.Success -> {
                NSURLSessionAuthChallengeUseCredential to NSURLCredential.credentialForTrust(serverTrust)
            }
            is VerificationResult.Failure -> {
                if (configuration.failOnError) {
                    NSURLSessionAuthChallengeCancelAuthenticationChallenge to null
                } else {
                    NSURLSessionAuthChallengePerformDefaultHandling to null
                }
            }
        }
    }
}
