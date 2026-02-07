package com.jermey.seal.core.loglist

/**
 * A [LogListDataSource] that loads the bundled baseline log list.
 * Used as a last-resort fallback when the network log list is unavailable
 * and no cached version exists.
 */
public class EmbeddedLogListDataSource : LogListDataSource {
    override suspend fun fetchLogList(): Result<RawLogList> {
        return try {
            val jsonBytes = EmbeddedLogListData.json.encodeToByteArray()
            Result.success(
                RawLogList(
                    jsonBytes = jsonBytes,
                    signatureBytes = null, // Embedded list has no separate signature
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
