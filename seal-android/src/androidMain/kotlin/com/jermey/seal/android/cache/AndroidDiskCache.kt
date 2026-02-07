package com.jermey.seal.android.cache

import android.content.Context
import com.jermey.seal.core.loglist.AndroidDiskLogListCache
import com.jermey.seal.core.loglist.CachedLogList
import com.jermey.seal.core.loglist.LogListCache

/**
 * Android disk cache for the CT log list backed by the app's cache directory.
 *
 * This is a convenience wrapper that uses Android [Context] to determine
 * the cache directory, delegating to [AndroidDiskLogListCache] from seal-core.
 *
 * Usage:
 * ```kotlin
 * val cache = AndroidDiskCache(context)
 * val config = ctConfiguration {
 *     logListCache = cache
 * }
 * ```
 */
public class AndroidDiskCache(context: Context) : LogListCache {

    private val delegate: AndroidDiskLogListCache =
        AndroidDiskLogListCache(context.cacheDir)

    override suspend fun get(): CachedLogList? = delegate.get()

    override suspend fun put(logList: CachedLogList) {
        delegate.put(logList)
    }
}
