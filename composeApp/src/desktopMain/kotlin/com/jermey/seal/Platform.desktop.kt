package com.jermey.seal

class DesktopPlatform : Platform {
    override val name: String =
        "JVM ${System.getProperty("os.name")} ${System.getProperty("os.version")}"
}

actual fun getPlatform(): Platform = DesktopPlatform()
