package com.jermey.seal.core.loglist

import kotlinx.datetime.Instant

/**
 * Cache interface for storing and retrieving a parsed log list.
 */
public interface LogListCache {
    /**
     * Get the cached log list, or null if not cached.
     */
    public suspend fun get(): CachedLogList?

    /**
     * Store a log list in the cache.
     */
    public suspend fun put(logList: CachedLogList)
}

/**
 * A cached log list with metadata about when it was fetched.
 *
 * @property jsonBytes The raw JSON bytes of the cached log list.
 * @property signatureBytes The optional signature bytes, or `null` if unavailable.
 * @property fetchedAt The instant at which this log list was fetched from its source.
 */
public data class CachedLogList(
    public val jsonBytes: ByteArray,
    public val signatureBytes: ByteArray?,
    public val fetchedAt: Instant,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CachedLogList) return false
        return jsonBytes.contentEquals(other.jsonBytes) &&
            ((signatureBytes == null && other.signatureBytes == null) ||
                (signatureBytes != null && other.signatureBytes != null &&
                    signatureBytes.contentEquals(other.signatureBytes))) &&
            fetchedAt == other.fetchedAt
    }

    override fun hashCode(): Int {
        var result = jsonBytes.contentHashCode()
        result = 31 * result + (signatureBytes?.contentHashCode() ?: 0)
        result = 31 * result + fetchedAt.hashCode()
        return result
    }
}
