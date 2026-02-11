package com.jermey.seal.jvm.trust

import com.jermey.seal.jvm.chain.CertificateChainCleaner
import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.crypto.createCryptoVerifier
import com.jermey.seal.core.loglist.InMemoryLogListCache
import com.jermey.seal.core.loglist.LogListService
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.verification.CertificateTransparencyVerifier
import kotlinx.coroutines.runBlocking
import java.net.Socket
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLSocket
import javax.net.ssl.X509ExtendedTrustManager
import javax.net.ssl.X509TrustManager

public class CTTrustManager internal constructor(
    private val delegate: X509TrustManager,
    private val configuration: CTConfiguration,
) : X509ExtendedTrustManager() {

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

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) {
        delegate.checkClientTrusted(chain, authType)
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
        delegate.checkServerTrusted(chain, authType)
        verifyCertificateTransparency(chain.toList(), host = null)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkClientTrusted(chain, authType, socket)
        } else {
            delegate.checkClientTrusted(chain, authType)
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, socket: Socket) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkServerTrusted(chain, authType, socket)
        } else {
            delegate.checkServerTrusted(chain, authType)
        }
        val host = (socket as? SSLSocket)?.let { sslSocket ->
            sslSocket.handshakeSession?.peerHost
        }
        verifyCertificateTransparency(chain.toList(), host)
    }

    override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkClientTrusted(chain, authType, engine)
        } else {
            delegate.checkClientTrusted(chain, authType)
        }
    }

    override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String, engine: SSLEngine) {
        if (delegate is X509ExtendedTrustManager) {
            delegate.checkServerTrusted(chain, authType, engine)
        } else {
            delegate.checkServerTrusted(chain, authType)
        }
        val host = engine.peerHost
        verifyCertificateTransparency(chain.toList(), host)
    }

    override fun getAcceptedIssuers(): Array<X509Certificate> =
        delegate.acceptedIssuers

    private fun verifyCertificateTransparency(chain: List<X509Certificate>, host: String?) {
        if (host != null && !configuration.hostMatcher.matches(host)) {
            configuration.logger?.invoke(host, VerificationResult.Success.DisabledForHost)
            return
        }
        val cleanedChain = CertificateChainCleaner.clean(chain)
        val derChain = cleanedChain.map { it.encoded }
        val result = runBlocking {
            verifier.verify(certificateChain = derChain)
        }
        host?.let { configuration.logger?.invoke(it, result) }
        if (result is VerificationResult.Failure && configuration.failOnError) {
            throw CertificateException(
                "Certificate Transparency verification failed${host?.let { " for $it" } ?: ""}: $result"
            )
        }
    }
}
