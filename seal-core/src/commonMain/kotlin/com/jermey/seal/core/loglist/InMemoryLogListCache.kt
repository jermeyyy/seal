package com.jermey.seal.core.loglist

/**
 * A simple in-memory implementation of [LogListCache].
 * Holds at most one cached log list in memory. Not persistent across app restarts.
 */
public class InMemoryLogListCache : LogListCache {
    private var cached: CachedLogList? = null

    override suspend fun get(): CachedLogList? = cached

    override suspend fun put(logList: CachedLogList) {
        cached = logList
    }
}
