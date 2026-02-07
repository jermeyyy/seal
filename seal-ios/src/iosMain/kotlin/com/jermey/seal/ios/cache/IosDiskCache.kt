package com.jermey.seal.ios.cache

import com.jermey.seal.core.loglist.CachedLogList
import com.jermey.seal.core.loglist.IosDiskLogListCache
import com.jermey.seal.core.loglist.LogListCache

/**
 * iOS disk cache for the CT log list backed by the app's caches directory.
 *
 * This is a convenience wrapper around [IosDiskLogListCache] from seal-core
 * that automatically uses the default iOS caches directory.
 *
 * Usage:
 * ```kotlin
 * val cache = IosDiskCache()
 * val config = ctConfiguration {
 *     logListCache = cache
 * }
 * ```
 */
public class IosDiskCache : LogListCache {

    private val delegate: IosDiskLogListCache = IosDiskLogListCache.createDefault()

    override suspend fun get(): CachedLogList? = delegate.get()

    override suspend fun put(logList: CachedLogList) {
        delegate.put(logList)
    }
}
