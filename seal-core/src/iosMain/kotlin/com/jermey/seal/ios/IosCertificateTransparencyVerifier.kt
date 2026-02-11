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

    public suspend fun verify(secTrust: SecTrustRef, host: String): VerificationResult {
        if (!configuration.hostMatcher.matches(host)) {
            return VerificationResult.Success.DisabledForHost.also {
                configuration.logger?.invoke(host, it)
            }
        }

        return try {
            val certChain = certExtractor.extractCertificates(secTrust)
            if (certChain.isEmpty()) {
                val result = VerificationResult.Failure.NoScts
                configuration.logger?.invoke(host, result)
                return result
            }

            val coreResult = coreVerifier.verify(certChain)

            if (coreResult is VerificationResult.Success) {
                configuration.logger?.invoke(host, coreResult)
                return coreResult
            }

            val osCtCompliant = ctChecker.checkCtCompliance(secTrust)

            if (osCtCompliant) {
                val result = VerificationResult.Success.OsVerified(
                    platform = "iOS/SecTrust",
                    ctConfirmed = true,
                    coreVerificationResult = coreResult,
                )
                configuration.logger?.invoke(host, result)
                return result
            }

            configuration.logger?.invoke(host, coreResult)
            coreResult
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            val result = VerificationResult.Failure.UnknownError(e)
            configuration.logger?.invoke(host, result)
            result
        }
    }
}
