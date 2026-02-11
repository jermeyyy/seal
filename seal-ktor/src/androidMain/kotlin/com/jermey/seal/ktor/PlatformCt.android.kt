package com.jermey.seal.ktor

import com.jermey.seal.jvm.okhttp.installCertificateTransparency
import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttpConfig
import io.ktor.client.plugins.api.ClientPluginBuilder

internal actual fun installPlatformCt(
    pluginInstance: ClientPluginBuilder<CTConfigurationBuilder>,
    config: CTConfiguration,
) {
    // On Android, CT verification is handled at the OkHttp engine level via
    // CertificateTransparencyInterceptor as a network interceptor.
    // Ktor's abstraction layer does not expose TLS certificate details, so
    // plugin-level hooks cannot perform certificate verification.
    //
    // Use the certificateTransparency {} extension on HttpClientConfig<OkHttpConfig>
    // for automatic OkHttp integration.
}

/**
 * Install Certificate Transparency verification for a Ktor [HttpClient][io.ktor.client.HttpClient]
 * using the OkHttp engine.
 *
 * Configures the OkHttp engine with a Conscrypt SSLSocketFactory that enables CT
 * and a [CertificateTransparencyInterceptor][com.jermey.seal.jvm.okhttp.CertificateTransparencyInterceptor]
 * as a network interceptor, performing CT verification on every HTTPS connection.
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient(OkHttp) {
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
public fun HttpClientConfig<OkHttpConfig>.certificateTransparency(
    block: CTConfigurationBuilder.() -> Unit = {},
) {
    engine {
        config {
            installCertificateTransparency(block)
        }
    }
}
