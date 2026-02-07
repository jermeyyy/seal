package com.jermey.seal.ktor

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import io.ktor.client.plugins.api.ClientPlugin
import io.ktor.client.plugins.api.ClientPluginBuilder
import io.ktor.client.plugins.api.createClientPlugin

/**
 * Ktor HttpClient plugin that adds Certificate Transparency verification.
 *
 * Usage:
 * ```kotlin
 * val client = HttpClient(OkHttp) { // or HttpClient(Darwin) on iOS
 *     install(CertificateTransparency) {
 *         +"*.example.com"
 *         -"internal.example.com"
 *         failOnError = false
 *     }
 * }
 * ```
 */
public val CertificateTransparency: ClientPlugin<CTConfigurationBuilder> = createClientPlugin(
    "CertificateTransparency",
    ::CTConfigurationBuilder,
) {
    val config = pluginConfig.build()
    installPlatformCt(this, config)
}

/**
 * Platform-specific CT installation hook.
 *
 * On Android (OkHttp engine): adds [CertificateTransparencyInterceptor] as a network interceptor.
 * On iOS (Darwin engine): hooks into `handleChallenge` for server trust evaluation.
 */
internal expect fun installPlatformCt(
    pluginInstance: ClientPluginBuilder<CTConfigurationBuilder>,
    config: CTConfiguration,
)
