package com.jermey.seal.ios.cache

import com.jermey.seal.core.loglist.CachedLogList
import com.jermey.seal.core.loglist.IosDiskLogListCache
import com.jermey.seal.core.loglist.LogListCache

public class IosDiskCache : LogListCache {

    private val delegate: IosDiskLogListCache = IosDiskLogListCache.createDefault()

    override suspend fun get(): CachedLogList? = delegate.get()

    override suspend fun put(logList: CachedLogList) {
        delegate.put(logList)
    }
}
