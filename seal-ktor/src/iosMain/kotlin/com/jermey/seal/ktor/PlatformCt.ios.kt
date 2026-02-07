package com.jermey.seal.ktor

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.ios.IosCertificateTransparencyVerifier
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.DarwinClientEngineConfig
import io.ktor.client.plugins.api.ClientPluginBuilder
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.UnsafeNumber
import kotlinx.cinterop.convert
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSURLAuthenticationMethodServerTrust
import platform.Foundation.NSURLCredential
import platform.Foundation.NSURLSessionAuthChallengeCancelAuthenticationChallenge
import platform.Foundation.NSURLSessionAuthChallengePerformDefaultHandling
import platform.Foundation.NSURLSessionAuthChallengeUseCredential
import platform.Foundation.credentialForTrust
import platform.Foundation.serverTrust
import platform.Security.SecTrustRef

internal actual fun installPlatformCt(
    pluginInstance: ClientPluginBuilder<CTConfigurationBuilder>,
    config: CTConfiguration,
) {
    // On iOS, CT verification is handled at the Darwin engine level via
    // handleChallenge for server trust evaluation.
    // Ktor's abstraction layer does not expose SecTrust objects through plugin hooks.
    //
    // Use the certificateTransparency {} extension on HttpClientConfig<DarwinClientEngineConfig>
    // for automatic Darwin engine integration.
}

/**
 * Install Certificate Transparency verification for a Ktor [HttpClient][io.ktor.client.HttpClient]
 * using the Darwin engine.
 *
 * Configures the Darwin engine's challenge handler to intercept server trust challenges
 * and verify CT compliance using [IosCertificateTransparencyVerifier].
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient(Darwin) {
 *     certificateTransparency {
 *         +"*.example.com"
 *         -"internal.example.com"
 *         failOnError = false
 *     }
 * }
 * ```
 *
 * @param block DSL configuration block for CT settings.
 */
@OptIn(ExperimentalForeignApi::class, UnsafeNumber::class)
public fun HttpClientConfig<DarwinClientEngineConfig>.certificateTransparency(
    block: CTConfigurationBuilder.() -> Unit = {},
) {
    val config = ctConfiguration(block)
    val verifier = IosCertificateTransparencyVerifier(config)

    engine {
        handleChallenge { _, _, challenge, completionHandler ->
            val protectionSpace = challenge.protectionSpace

            // Only handle ServerTrust challenges
            if (protectionSpace.authenticationMethod != NSURLAuthenticationMethodServerTrust) {
                completionHandler(
                    NSURLSessionAuthChallengePerformDefaultHandling.convert(),
                    null,
                )
                return@handleChallenge
            }

            val serverTrust: SecTrustRef? = protectionSpace.serverTrust

            if (serverTrust == null) {
                if (config.failOnError) {
                    completionHandler(
                        NSURLSessionAuthChallengeCancelAuthenticationChallenge.convert(),
                        null,
                    )
                } else {
                    completionHandler(
                        NSURLSessionAuthChallengePerformDefaultHandling.convert(),
                        null,
                    )
                }
                return@handleChallenge
            }

            val host = protectionSpace.host

            // Run CT verification
            val result = runBlocking {
                verifier.verify(serverTrust, host)
            }

            when {
                result is VerificationResult.Success -> {
                    // CT verification passed — accept the server trust
                    completionHandler(
                        NSURLSessionAuthChallengeUseCredential.convert(),
                        NSURLCredential.credentialForTrust(serverTrust),
                    )
                }
                result is VerificationResult.Failure && config.failOnError -> {
                    // CT verification failed and fail-on-error is enabled
                    completionHandler(
                        NSURLSessionAuthChallengeCancelAuthenticationChallenge.convert(),
                        null,
                    )
                }
                else -> {
                    // CT verification failed but fail-open — accept anyway
                    completionHandler(
                        NSURLSessionAuthChallengePerformDefaultHandling.convert(),
                        null,
                    )
                }
            }
        }
    }
}
