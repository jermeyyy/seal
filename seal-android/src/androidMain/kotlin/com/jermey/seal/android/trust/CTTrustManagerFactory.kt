package com.jermey.seal.android.trust

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Factory for creating [CTTrustManager] instances.
 *
 * Usage:
 * ```kotlin
 * val trustManager = CTTrustManagerFactory.create {
 *     +"*.example.com"
 *     failOnError = true
 * }
 *
 * val sslContext = SSLContext.getInstance("TLS").apply {
 *     init(null, arrayOf(trustManager), null)
 * }
 * ```
 */
public object CTTrustManagerFactory {

    /**
     * Create a [CTTrustManager] that wraps the system default trust manager.
     *
     * @param block DSL configuration block.
     * @return A [CTTrustManager] that adds CT verification.
     */
    public fun create(block: CTConfigurationBuilder.() -> Unit = {}): CTTrustManager {
        val config = ctConfiguration(block)
        return create(config)
    }

    /**
     * Create a [CTTrustManager] with an existing configuration.
     */
    public fun create(configuration: CTConfiguration): CTTrustManager {
        val systemTrustManager = getSystemTrustManager()
        return CTTrustManager(systemTrustManager, configuration)
    }

    private fun getSystemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .first()
    }
}
