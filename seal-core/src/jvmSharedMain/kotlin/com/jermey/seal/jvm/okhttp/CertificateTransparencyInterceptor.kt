package com.jermey.seal.jvm.okhttp

import com.jermey.seal.jvm.chain.CertificateChainCleaner
import com.jermey.seal.jvm.conscrypt.ConscryptSctExtractor
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
        val response = chain.proceed(request)
        if (!configuration.hostMatcher.matches(host)) {
            configuration.logger?.invoke(host, VerificationResult.Success.DisabledForHost)
            return response
        }
        val connection = chain.connection() ?: return response
        val handshake = connection.handshake() ?: return response
        val peerCertificates = handshake.peerCertificates
        if (peerCertificates.isEmpty()) return response
        val x509Certs = peerCertificates.filterIsInstance<X509Certificate>()
        if (x509Certs.isEmpty()) return response
        val cleanedChain = CertificateChainCleaner.clean(x509Certs)
        val derChain = cleanedChain.map { it.encoded }
        val socket = connection.socket()
        val tlsExtensionSctBytes: ByteArray? = if (socket is SSLSocket) {
            sctExtractor.getRawTlsSctData(socket)
        } else {
            null
        }
        val ocspResponseBytes: ByteArray? = if (socket is SSLSocket) {
            sctExtractor.getRawOcspResponseData(socket)
        } else {
            null
        }
        val result = runBlocking {
            verifier.verify(
                certificateChain = derChain,
                tlsExtensionSctBytes = tlsExtensionSctBytes,
                ocspResponseSctBytes = ocspResponseBytes,
            )
        }
        configuration.logger?.invoke(host, result)
        if (result is VerificationResult.Failure) {
            if (configuration.failOnError) {
                response.close()
                throw SSLPeerUnverifiedException(
                    "Certificate Transparency verification failed for $host: $result"
                )
            }
        }
        return response
    }
}
