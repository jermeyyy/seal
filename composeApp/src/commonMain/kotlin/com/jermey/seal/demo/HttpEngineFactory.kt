package com.jermey.seal.demo

import com.jermey.seal.core.config.CTConfigurationBuilder
import io.ktor.client.HttpClient

/**
 * Creates a platform-specific Ktor [HttpClient] with Certificate Transparency
 * verification configured using the proper engine-level integration.
 *
 * On Android, uses OkHttp engine with [CertificateTransparencyInterceptor].
 * On iOS, uses Darwin engine with server trust challenge handler.
 *
 * @param ctConfig DSL configuration block for CT settings.
 * @param httpConfig Additional HttpClient configuration block.
 */
expect fun createCtHttpClient(
    ctConfig: CTConfigurationBuilder.() -> Unit = {},
    httpConfig: io.ktor.client.HttpClientConfig<*>.() -> Unit = {},
): HttpClient
