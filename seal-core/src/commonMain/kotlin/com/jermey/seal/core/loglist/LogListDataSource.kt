package com.jermey.seal.core.loglist

/**
 * Abstraction for fetching a raw CT log list (JSON + optional signature).
 */
public interface LogListDataSource {
    /**
     * Fetch the raw log list data.
     * @return A [Result] containing the raw log list JSON and optional signature bytes.
     */
    public suspend fun fetchLogList(): Result<RawLogList>
}

/**
 * Raw log list data as fetched from a source.
 * @property jsonBytes The raw JSON bytes of the log list
 * @property signatureBytes The optional signature bytes (from the .sig file)
 */
public data class RawLogList(
    public val jsonBytes: ByteArray,
    public val signatureBytes: ByteArray?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RawLogList) return false
        return jsonBytes.contentEquals(other.jsonBytes) &&
            ((signatureBytes == null && other.signatureBytes == null) ||
                (signatureBytes != null && other.signatureBytes != null &&
                    signatureBytes.contentEquals(other.signatureBytes)))
    }

    override fun hashCode(): Int {
        var result = jsonBytes.contentHashCode()
        result = 31 * result + (signatureBytes?.contentHashCode() ?: 0)
        return result
    }
}
