package com.jermey.seal.core.loglist

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.dataWithContentsOfFile
import platform.posix.memcpy

/**
 * iOS implementation of [ResourceLoader].
 * Loads resources from the main application bundle.
 */
@OptIn(ExperimentalForeignApi::class)
public actual object ResourceLoader {
    public actual fun loadResource(name: String): ByteArray {
        val baseName = name.substringBeforeLast(".")
        val extension = name.substringAfterLast(".")

        val path = NSBundle.mainBundle.pathForResource(baseName, ofType = extension)
            ?: throw IllegalStateException("Resource not found in bundle: $name")

        val data = NSData.dataWithContentsOfFile(path)
            ?: throw IllegalStateException("Failed to read resource data: $name")

        val size = data.length.toInt()
        if (size == 0) return ByteArray(0)

        val bytes = ByteArray(size)
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), data.bytes, data.length)
        }
        return bytes
    }
}
