package com.jermey.seal.android.cache

import android.content.Context
import com.jermey.seal.core.loglist.AndroidDiskLogListCache
import com.jermey.seal.core.loglist.CachedLogList
import com.jermey.seal.core.loglist.LogListCache

public class AndroidDiskCache(context: Context) : LogListCache {
    private val delegate: AndroidDiskLogListCache =
        AndroidDiskLogListCache(context.cacheDir)

    override suspend fun get(): CachedLogList? = delegate.get()

    override suspend fun put(logList: CachedLogList) {
        delegate.put(logList)
    }
}
