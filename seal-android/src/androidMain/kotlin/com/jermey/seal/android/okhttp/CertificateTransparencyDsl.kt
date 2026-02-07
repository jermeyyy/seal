package com.jermey.seal.android.okhttp

import com.jermey.seal.core.config.CTConfigurationBuilder
import com.jermey.seal.core.config.ctConfiguration
import okhttp3.Interceptor

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
