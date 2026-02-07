package com.jermey.seal.demo

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp

actual fun createPlatformHttpEngine(): HttpClientEngine = OkHttp.create()
