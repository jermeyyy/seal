package com.jermey.seal.demo

import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.ktor.certificateTransparency
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.darwin.Darwin

actual fun createCtHttpClient(
    ctConfig: CTConfigurationBuilder.() -> Unit,
    httpConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Darwin) {
    certificateTransparency(ctConfig)
    httpConfig()
}
