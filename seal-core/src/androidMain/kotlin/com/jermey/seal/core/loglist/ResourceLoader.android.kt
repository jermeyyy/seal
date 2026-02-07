package com.jermey.seal.core.loglist

/**
 * Android implementation of [ResourceLoader].
 * Loads resources from the classpath using the class loader.
 */
public actual object ResourceLoader {
    public actual fun loadResource(name: String): ByteArray {
        val stream = ResourceLoader::class.java.classLoader
            ?.getResourceAsStream(name)
            ?: throw IllegalStateException("Resource not found: $name")
        return stream.use { it.readBytes() }
    }
}
