package com.jermey.seal.jvm.trust

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import java.security.KeyStore
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

public object CTTrustManagerFactory {
    public fun create(block: CTConfigurationBuilder.() -> Unit = {}): CTTrustManager {
        val config = ctConfiguration(block)
        return create(config)
    }

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
