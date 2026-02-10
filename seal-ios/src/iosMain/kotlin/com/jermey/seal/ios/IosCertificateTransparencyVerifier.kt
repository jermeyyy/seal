package com.jermey.seal.ios

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.crypto.createCryptoVerifier
import com.jermey.seal.core.loglist.InMemoryLogListCache
import com.jermey.seal.core.loglist.LogListService
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.verification.CertificateTransparencyVerifier
import com.jermey.seal.ios.sectrust.SecTrustCertificateExtractor
import com.jermey.seal.ios.sectrust.SecTrustCtChecker
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Security.SecTrustRef

/**
 * iOS-specific Certificate Transparency verifier that combines:
 * - Manual embedded SCT verification via seal-core's [CertificateTransparencyVerifier]
 * - OS-level TLS/OCSP CT compliance checking via [SecTrustCtChecker]
 *
 * Usage:
 * ```kotlin
 * val verifier = IosCertificateTransparencyVerifier(configuration)
 * val result = verifier.verify(serverTrust, host)
 * ```
 */
@OptIn(ExperimentalForeignApi::class)
public class IosCertificateTransparencyVerifier(
    private val configuration: CTConfiguration,
) {
    private val ctChecker = SecTrustCtChecker()
    private val certExtractor = SecTrustCertificateExtractor()

    private val logListService = LogListService(
        networkSource = configuration.logListNetworkDataSource,
        cache = configuration.logListCache ?: InMemoryLogListCache(),
        maxAge = configuration.logListMaxAge,
    )

    private val coreVerifier = CertificateTransparencyVerifier(
        cryptoVerifier = createCryptoVerifier(),
        logListService = logListService,
        policy = configuration.policy,
    )

    /**
     * Verify CT compliance for a server trust object.
     *
     * Combines manual embedded-SCT verification with OS-level TLS/OCSP checking:
     * 1. Checks host exclusion via [CTConfiguration.hostMatcher]
     * 2. Extracts DER certificates from the [SecTrustRef]
     * 3. Verifies embedded SCTs via [CertificateTransparencyVerifier]
     * 4. Falls back to OS-level CT compliance via [SecTrustCtChecker]
     *
     * @param secTrust The SecTrust from the TLS handshake
     * @param host The hostname being connected to (for include/exclude checks)
     * @return [VerificationResult] indicating CT compliance
     */
    public suspend fun verify(secTrust: SecTrustRef, host: String): VerificationResult {
        // 1. Check host exclusion
        if (!configuration.hostMatcher.matches(host)) {
            return VerificationResult.Success.DisabledForHost.also {
                configuration.logger?.invoke(host, it)
            }
        }

        return try {
            // 2. Extract certificates
            val certChain = certExtractor.extractCertificates(secTrust)
            if (certChain.isEmpty()) {
                val result = VerificationResult.Failure.NoScts
                configuration.logger?.invoke(host, result)
                return result
            }

            // 3. Verify embedded SCTs via core verifier
            val coreResult = coreVerifier.verify(certChain)

            // 4. If core verification succeeded, use that
            if (coreResult is VerificationResult.Success) {
                configuration.logger?.invoke(host, coreResult)
                return coreResult
            }

            // 5. Check OS-level CT compliance (handles TLS/OCSP SCTs)
            val osCtCompliant = ctChecker.checkCtCompliance(secTrust)

            if (osCtCompliant) {
                // OS reports CT compliance (TLS/OCSP SCTs verified by system)
                val result = VerificationResult.Success.OsVerified(
                    platform = "iOS/SecTrust",
                    ctConfirmed = true,
                    coreVerificationResult = coreResult,
                )
                configuration.logger?.invoke(host, result)
                return result
            }

            // 6. Both failed — return core verifier's failure
            configuration.logger?.invoke(host, coreResult)
            coreResult
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val result = VerificationResult.Failure.UnknownError(e)
            configuration.logger?.invoke(host, result)
            result
        }
    }
}
