package com.jermey.seal.core.loglist

import kotlinx.datetime.Instant
import java.io.File
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private const val CACHE_SUBDIR = "ct-log-cache"
private const val JSON_FILE = "log_list.json"
private const val META_FILE = "log_list.meta"
private const val META_KEY_FETCHED_AT = "fetchedAt"
private const val META_KEY_SIGNATURE = "signature"

/**
 * A persistent disk-based implementation of [LogListCache] for JVM desktop.
 *
 * Stores the cached log list in a `ct-log-cache/` subdirectory of the provided [cacheDir].
 * Defaults to `~/.seal/cache` if no directory is specified.
 */
@OptIn(ExperimentalEncodingApi::class)
public class JvmDiskLogListCache(
    cacheDir: File = File(System.getProperty("user.home"), ".seal/cache"),
) : LogListCache {

    private val cacheDirectory: File = File(cacheDir, CACHE_SUBDIR)

    override suspend fun get(): CachedLogList? {
        return try {
            val jsonFile = File(cacheDirectory, JSON_FILE)
            val metaFile = File(cacheDirectory, META_FILE)
            if (!jsonFile.exists() || !metaFile.exists()) return null

            val jsonBytes = jsonFile.readBytes()
            val metaLines = metaFile.readLines()
            val metaMap = metaLines
                .filter { it.contains('=') }
                .associate { line ->
                    val key = line.substringBefore('=')
                    val value = line.substringAfter('=')
                    key to value
                }

            val fetchedAt = Instant.parse(
                metaMap[META_KEY_FETCHED_AT]
                    ?: return null,
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
            cacheDirectory.mkdirs()

            File(cacheDirectory, JSON_FILE).writeBytes(logList.jsonBytes)

            val signatureEncoded = logList.signatureBytes?.let { Base64.encode(it) } ?: ""
            val metaContent = buildString {
                appendLine("$META_KEY_FETCHED_AT=${logList.fetchedAt}")
                appendLine("$META_KEY_SIGNATURE=$signatureEncoded")
            }
            File(cacheDirectory, META_FILE).writeText(metaContent)
        } catch (_: Exception) {
            // Silently ignore write errors — cache is best-effort.
        }
    }
}
