package com.jermey.seal.core.loglist

/**
 * Platform-specific loader for embedded resources.
 */
public expect object ResourceLoader {
    /**
     * Load a named resource as a byte array.
     * @param name The resource name (without path prefix)
     * @return The resource contents as a byte array
     * @throws IllegalStateException if the resource cannot be found
     */
    public fun loadResource(name: String): ByteArray
}
