package com.jermey.seal

class WasmJsPlatform : Platform {
    override val name: String = "Web Browser"
}

actual fun getPlatform(): Platform = WasmJsPlatform()
