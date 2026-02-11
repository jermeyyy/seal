package com.jermey.seal.jvm.okhttp

import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import com.jermey.seal.jvm.ConscryptInitializer
import okhttp3.Interceptor
import okhttp3.OkHttpClient

public fun certificateTransparencyInterceptor(
    block: CTConfigurationBuilder.() -> Unit = {}
): Interceptor {
    val config = ctConfiguration(block)
    return CertificateTransparencyInterceptor(config)
}

public fun OkHttpClient.Builder.installCertificateTransparency(
    block: CTConfigurationBuilder.() -> Unit = {}
): OkHttpClient.Builder {
    val config = ctConfiguration(block)
    val trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
        javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
    )
    trustManagerFactory.init(null as java.security.KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers
    val x509TrustManager = trustManagers.first { it is javax.net.ssl.X509TrustManager } as javax.net.ssl.X509TrustManager

    if (ConscryptInitializer.isAvailable) {
        val sslContext = javax.net.ssl.SSLContext.getInstance("TLS", org.conscrypt.Conscrypt.newProvider())
        sslContext.init(null, arrayOf(x509TrustManager), null)
        val baseFactory = sslContext.socketFactory
        val ctSocketFactory = ConscryptCtSocketFactory(baseFactory)
        sslSocketFactory(ctSocketFactory, x509TrustManager)
    }

    addNetworkInterceptor(CertificateTransparencyInterceptor(config))
    return this
}
