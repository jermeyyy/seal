package com.jermey.seal.android.okhttp

import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import okhttp3.Interceptor
import okhttp3.OkHttpClient

/**
 * Create an OkHttp network interceptor that enforces Certificate Transparency.
 *
 * Usage:
 * ```kotlin
 * val client = OkHttpClient.Builder()
 *     .addNetworkInterceptor(
 *         certificateTransparencyInterceptor {
 *             +"*.example.com"
 *             -"internal.example.com"
 *             failOnError = false
 *         }
 *     )
 *     .build()
 * ```
 *
 * @param block DSL configuration block.
 * @return An [Interceptor] that performs CT verification.
 */
public fun certificateTransparencyInterceptor(
    block: CTConfigurationBuilder.() -> Unit = {}
): Interceptor {
    val config = ctConfiguration(block)
    return CertificateTransparencyInterceptor(config)
}

/**
 * Configure an OkHttpClient.Builder with Certificate Transparency verification.
 *
 * This sets up both:
 * 1. A Conscrypt SSLSocketFactory that enables CT (so SCTs are available via TLS extension)
 * 2. A network interceptor that verifies the SCTs
 *
 * @param block DSL configuration block.
 */
public fun OkHttpClient.Builder.installCertificateTransparency(
    block: CTConfigurationBuilder.() -> Unit = {}
): OkHttpClient.Builder {
    val config = ctConfiguration(block)

    // Configure Conscrypt SSL socket factory with CT enabled
    val trustManagerFactory = javax.net.ssl.TrustManagerFactory.getInstance(
        javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
    )
    trustManagerFactory.init(null as java.security.KeyStore?)
    val trustManagers = trustManagerFactory.trustManagers
    val x509TrustManager = trustManagers.first { it is javax.net.ssl.X509TrustManager } as javax.net.ssl.X509TrustManager

    val sslContext = javax.net.ssl.SSLContext.getInstance("TLS", org.conscrypt.Conscrypt.newProvider())
    sslContext.init(null, arrayOf(x509TrustManager), null)

    val baseFactory = sslContext.socketFactory
    val ctSocketFactory = ConscryptCtSocketFactory(baseFactory)

    sslSocketFactory(ctSocketFactory, x509TrustManager)
    addNetworkInterceptor(CertificateTransparencyInterceptor(config))

    return this
}
