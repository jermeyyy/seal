package com.jermey.seal.core.loglist

import com.jermey.seal.core.model.LogServer
import kotlinx.datetime.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Result of a log list fetch operation.
 */
public sealed class LogListResult {
    /**
     * Successfully loaded log servers.
     * @property servers The list of trusted log servers
     * @property source Where the log list was loaded from
     * @property isStale Whether the log list is older than maxAge
     */
    public data class Success(
        public val servers: List<LogServer>,
        public val source: Source,
        public val isStale: Boolean = false,
    ) : LogListResult()

    /**
     * Failed to load from any source.
     */
    public data class Failure(
        public val error: Throwable,
    ) : LogListResult()

    /**
     * Where the log list was loaded from.
     */
    public enum class Source {
        CACHE,
        NETWORK,
        EMBEDDED,
    }
}

/**
 * Service that manages the lifecycle of the trusted CT log list.
 *
 * Loading priority: Cache → Network → Embedded fallback.
 * If the cached list is older than [maxAge], a network refresh is attempted.
 * If that fails, the stale cache is used with a warning.
 * If no cache exists, the embedded baseline is used.
 */
public class LogListService(
    private val networkSource: LogListDataSource? = null,
    private val embeddedSource: LogListDataSource = EmbeddedLogListDataSource(),
    private val cache: LogListCache = InMemoryLogListCache(),
    private val signatureVerifier: LogListSignatureVerifier? = null,
    private val maxAge: Duration = 70.days,
    private val clock: Clock = Clock.System,
) {

    /**
     * Get the trusted log list from the best available source.
     *
     * Priority:
     * 1. Fresh cache (if within maxAge)
     * 2. Network fetch (with optional signature verification)
     * 3. Stale cache (with isStale = true)
     * 4. Embedded fallback
     */
    public suspend fun getLogList(): LogListResult {
        // 1. Try cache first
        val cached = cache.get()
        if (cached != null) {
            val age = clock.now() - cached.fetchedAt
            if (age < maxAge) {
                // Fresh cache
                return parseLogList(cached.jsonBytes, LogListResult.Source.CACHE, isStale = false)
            }
        }

        // 2. Try network
        if (networkSource != null) {
            val networkResult = tryNetworkFetch()
            if (networkResult != null) return networkResult
        }

        // 3. Use stale cache if available
        if (cached != null) {
            return parseLogList(cached.jsonBytes, LogListResult.Source.CACHE, isStale = true)
        }

        // 4. Fall back to embedded
        return tryEmbeddedFetch()
    }

    private suspend fun tryNetworkFetch(): LogListResult? {
        return try {
            val raw = networkSource?.fetchLogList()?.getOrNull() ?: return null

            // Verify signature if verifier is available and signature is present
            if (signatureVerifier != null && raw.signatureBytes != null) {
                val valid = signatureVerifier.verify(raw.jsonBytes, raw.signatureBytes)
                if (!valid) return null // Signature invalid, skip network result
            }

            // Parse to verify it's valid JSON
            val servers = LogListParser.parseToLogServers(raw.jsonBytes.decodeToString())
            if (servers.isEmpty()) return null

            // Cache the result
            val cachedLogList = CachedLogList(
                jsonBytes = raw.jsonBytes,
                signatureBytes = raw.signatureBytes,
                fetchedAt = clock.now(),
            )
            cache.put(cachedLogList)

            LogListResult.Success(
                servers = servers,
                source = LogListResult.Source.NETWORK,
                isStale = false,
            )
        } catch (_: Exception) {
            null
        }
    }

    private suspend fun tryEmbeddedFetch(): LogListResult {
        return try {
            val raw = embeddedSource.fetchLogList().getOrThrow()
            parseLogList(raw.jsonBytes, LogListResult.Source.EMBEDDED, isStale = false)
        } catch (e: Exception) {
            LogListResult.Failure(e)
        }
    }

    private fun parseLogList(
        jsonBytes: ByteArray,
        source: LogListResult.Source,
        isStale: Boolean,
    ): LogListResult {
        return try {
            val servers = LogListParser.parseToLogServers(jsonBytes.decodeToString())
            LogListResult.Success(
                servers = servers,
                source = source,
                isStale = isStale,
            )
        } catch (e: Exception) {
            LogListResult.Failure(e)
        }
    }
}
