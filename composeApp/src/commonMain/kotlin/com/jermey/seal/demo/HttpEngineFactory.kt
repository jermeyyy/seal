package com.jermey.seal.demo

import io.ktor.client.engine.HttpClientEngine

/**
 * Creates the platform-specific Ktor [HttpClientEngine].
 * Returns OkHttp on Android, Darwin on iOS.
 */
expect fun createPlatformHttpEngine(): HttpClientEngine
