package com.jermey.seal.core.loglist

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.datetime.Instant
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSString
import platform.Foundation.NSCachesDirectory
import platform.Foundation.NSUserDomainMask
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataWithContentsOfFile
import platform.Foundation.writeToFile
import platform.posix.memcpy
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val CACHE_SUBDIR = "ct-log-cache"
private const val JSON_FILE = "log_list.json"
private const val META_FILE = "log_list.meta"
private const val META_KEY_FETCHED_AT = "fetchedAt"
private const val META_KEY_SIGNATURE = "signature"

/**
 * A persistent disk-based implementation of [LogListCache] for iOS.
 *
 * Stores the cached log list as two files inside a `ct-log-cache/` subdirectory
 * of the provided [cacheDirectoryPath]:
 * - `log_list.json` — raw JSON bytes
 * - `log_list.meta` — metadata (fetchedAt timestamp + optional base64-encoded signature)
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
public class IosDiskLogListCache(
    cacheDirectoryPath: String,
) : LogListCache {

    private val cacheDirectory: String = "$cacheDirectoryPath/$CACHE_SUBDIR"

    public companion object {
        /**
         * Creates an [IosDiskLogListCache] using the default iOS caches directory.
         */
        public fun createDefault(): IosDiskLogListCache {
            @Suppress("UNCHECKED_CAST")
            val paths = NSSearchPathForDirectoriesInDomains(
                NSCachesDirectory,
                NSUserDomainMask,
                true,
            ) as List<String>
            val cachesDir = paths.first()
            return IosDiskLogListCache(cachesDir)
        }
    }

    override suspend fun get(): CachedLogList? {
        return try {
            val jsonPath = "$cacheDirectory/$JSON_FILE"
            val metaPath = "$cacheDirectory/$META_FILE"

            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(jsonPath) || !fileManager.fileExistsAtPath(metaPath)) {
                return null
            }

            val jsonData = NSData.dataWithContentsOfFile(jsonPath) ?: return null
            val jsonBytes = jsonData.toByteArray()

            val metaData = NSData.dataWithContentsOfFile(metaPath) ?: return null
            val metaString = NSString.create(data = metaData, encoding = NSUTF8StringEncoding)
                ?.toString() ?: return null

            val metaMap = metaString.lines()
                .filter { it.contains('=') }
                .associate { line ->
                    val key = line.substringBefore('=')
                    val value = line.substringAfter('=')
                    key to value
                }

            val fetchedAt = Instant.parse(
                metaMap[META_KEY_FETCHED_AT] ?: return null,
            )
            val signatureBase64 = metaMap[META_KEY_SIGNATURE]
            val signatureBytes = if (!signatureBase64.isNullOrEmpty()) {
                Base64.decode(signatureBase64)
            } else {
                null
            }

            CachedLogList(
                jsonBytes = jsonBytes,
                signatureBytes = signatureBytes,
                fetchedAt = fetchedAt,
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun put(logList: CachedLogList) {
        try {
            val fileManager = NSFileManager.defaultManager
            if (!fileManager.fileExistsAtPath(cacheDirectory)) {
                fileManager.createDirectoryAtPath(
                    cacheDirectory,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = null,
                )
            }

            val jsonPath = "$cacheDirectory/$JSON_FILE"
            val jsonData = logList.jsonBytes.toNSData()
            jsonData.writeToFile(jsonPath, atomically = true)

            val signatureEncoded = logList.signatureBytes?.let { Base64.encode(it) } ?: ""
            val metaContent = buildString {
                appendLine("$META_KEY_FETCHED_AT=${logList.fetchedAt}")
                appendLine("$META_KEY_SIGNATURE=$signatureEncoded")
            }
            val metaNSString = NSString.create(string = metaContent)
            metaNSString.writeToFile(metaPath(), atomically = true, encoding = NSUTF8StringEncoding, error = null)
        } catch (_: Exception) {
            // Silently ignore write errors — cache is best-effort.
        }
    }

    private fun metaPath(): String = "$cacheDirectory/$META_FILE"
}

@OptIn(ExperimentalForeignApi::class)
private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) return ByteArray(0)
    val bytes = ByteArray(size)
    bytes.usePinned { pinned ->
        memcpy(pinned.addressOf(0), this.bytes, this.length)
    }
    return bytes
}

@OptIn(ExperimentalForeignApi::class)
private fun ByteArray.toNSData(): NSData {
    if (isEmpty()) {
        return usePinned { pinned ->
            NSData.create(bytes = pinned.addressOf(0), length = 0u)
        }
    }
    return usePinned { pinned ->
        NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
    }
}
