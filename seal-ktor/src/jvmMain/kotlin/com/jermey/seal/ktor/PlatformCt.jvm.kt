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
    // On JVM desktop, CT verification is handled at the OkHttp engine level via
    // CertificateTransparencyInterceptor as a network interceptor.
    // Same approach as Android — both share jvmSharedMain code.
}

/**
 * Install Certificate Transparency verification for a Ktor [HttpClient][io.ktor.client.HttpClient]
 * using the OkHttp engine on JVM desktop.
 *
 * Configures the OkHttp engine with a Conscrypt SSLSocketFactory that enables CT
 * and a CertificateTransparencyInterceptor as a network interceptor.
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
