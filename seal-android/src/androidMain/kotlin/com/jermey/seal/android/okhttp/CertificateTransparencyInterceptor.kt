package com.jermey.seal.android.okhttp

import com.jermey.seal.android.chain.CertificateChainCleaner
import com.jermey.seal.android.conscrypt.ConscryptSctExtractor
import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.crypto.createCryptoVerifier
import com.jermey.seal.core.loglist.InMemoryLogListCache
import com.jermey.seal.core.loglist.LogListService
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.verification.CertificateTransparencyVerifier
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.security.cert.X509Certificate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.net.ssl.SSLSocket

/**
 * OkHttp network interceptor that enforces Certificate Transparency.
 *
 * Must be added as a **network interceptor** (not an application interceptor)
 * to have access to the TLS connection details.
 *
 * Usage:
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addNetworkInterceptor(CertificateTransparencyInterceptor(config))
 *     .build()
 * ```
 */
public class CertificateTransparencyInterceptor internal constructor(
    private val configuration: CTConfiguration,
) : Interceptor {

    private val sctExtractor: ConscryptSctExtractor = ConscryptSctExtractor()

    private val verifier: CertificateTransparencyVerifier by lazy {
        val logListService = LogListService(
            networkSource = configuration.logListNetworkDataSource,
            cache = configuration.logListCache ?: InMemoryLogListCache(),
            maxAge = configuration.logListMaxAge,
        )
        CertificateTransparencyVerifier(
            cryptoVerifier = createCryptoVerifier(),
            logListService = logListService,
            policy = configuration.policy,
        )
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val host = request.url.host

        // Step 1: Proceed with the request to establish the TLS connection
        val response = chain.proceed(request)

        // Step 2: Check host matching — skip verification if host is not included
        if (!configuration.hostMatcher.matches(host)) {
            configuration.logger?.invoke(host, VerificationResult.Success.DisabledForHost)
            return response
        }

        // Step 3: Get TLS connection details
        val connection = chain.connection() ?: return response
        val handshake = connection.handshake() ?: return response

        // Step 4: Get peer certificate chain
        val peerCertificates = handshake.peerCertificates
        if (peerCertificates.isEmpty()) return response

        val x509Certs = peerCertificates.filterIsInstance<X509Certificate>()
        if (x509Certs.isEmpty()) return response

        // Step 5: Clean and order the certificate chain
        val cleanedChain = CertificateChainCleaner.clean(x509Certs)

        // Step 6: Convert to DER byte arrays (leaf first)
        val derChain = cleanedChain.map { it.encoded }

        // Step 7: Extract raw TLS extension SCT bytes (if socket is Conscrypt SSLSocket)
        val socket = connection.socket()
        val tlsExtensionSctBytes: ByteArray? = if (socket is SSLSocket) {
            sctExtractor.getRawTlsSctData(socket)
        } else {
            null
        }

        // Step 8: Run verification
        // The verifier internally extracts embedded SCTs from the leaf certificate.
        // TLS extension SCTs are passed as raw bytes.
        // OCSP SCTs are not passed here because Conscrypt returns multiple OCSP responses
        // (one per cert) which cannot be directly mapped to the verifier's single-byte-array API.
        val result = runBlocking {
            verifier.verify(
                certificateChain = derChain,
                tlsExtensionSctBytes = tlsExtensionSctBytes,
            )
        }

        // Step 9: Invoke logger callback
        configuration.logger?.invoke(host, result)

        // Step 10: Handle failure
        if (result is VerificationResult.Failure) {
            if (configuration.failOnError) {
                response.close()
                throw SSLPeerUnverifiedException(
                    "Certificate Transparency verification failed for $host: $result"
                )
            }
            // Fail-open: log warning but return response
        }

        return response
    }
}
