package com.jermey.seal.demo

import com.jermey.seal.core.config.CTConfigurationBuilder
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.js.Js

actual fun createCtHttpClient(
    ctConfig: CTConfigurationBuilder.() -> Unit,
    httpConfig: HttpClientConfig<*>.() -> Unit,
): HttpClient = HttpClient(Js) {
    // No CT plugin needed — browser handles Certificate Transparency natively
    httpConfig()
}
