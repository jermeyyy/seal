package com.jermey.seal.ktor

import com.jermey.seal.core.config.CTConfiguration
import com.jermey.seal.core.config.CTConfigurationBuilder
import io.ktor.client.plugins.api.ClientPluginBuilder

internal actual fun installPlatformCt(
    pluginInstance: ClientPluginBuilder<CTConfigurationBuilder>,
    config: CTConfiguration,
) {
    // On wasmJs (browser), Certificate Transparency is handled natively by the browser.
    // No additional CT verification is needed or possible at the application level.
}
