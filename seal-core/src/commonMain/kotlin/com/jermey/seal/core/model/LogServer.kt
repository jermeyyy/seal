package com.jermey.seal.core.model

import kotlinx.datetime.Instant

/**
 * Represents a single Certificate Transparency log server from the CT log list.
 *
 * Each log server is identified by its [logId] (SHA-256 hash of its public key)
 * and has a lifecycle [state] that determines whether SCTs it issues are trusted.
 *
 * @property logId The unique identifier of this log (SHA-256 of the public key).
 * @property publicKey DER-encoded SubjectPublicKeyInfo of the log's signing key.
 * @property operator The name of the operator managing this log.
 * @property url The submission URL of this log server.
 * @property state The current lifecycle state of this log.
 * @property temporalInterval Optional time range during which this log accepts certificates.
 * @see LogOperator
 * @see LogState
 */
public data class LogServer(
    public val logId: LogId,
    public val publicKey: ByteArray,
    public val operator: String,
    public val url: String,
    public val state: LogState,
    public val temporalInterval: TemporalInterval?,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogServer) return false
        return logId == other.logId &&
            publicKey.contentEquals(other.publicKey) &&
            operator == other.operator &&
            url == other.url &&
            state == other.state &&
            temporalInterval == other.temporalInterval
    }

    override fun hashCode(): Int {
        var result = logId.hashCode()
        result = 31 * result + publicKey.contentHashCode()
        result = 31 * result + operator.hashCode()
        result = 31 * result + url.hashCode()
        result = 31 * result + state.hashCode()
        result = 31 * result + (temporalInterval?.hashCode() ?: 0)
        return result
    }
}

/**
 * Defines the time range during which a CT log accepts certificates.
 *
 * Certificates with a `notBefore` date outside this interval are not accepted
 * by the corresponding log server.
 *
 * @property startInclusive The start of the acceptance window (inclusive).
 * @property endExclusive The end of the acceptance window (exclusive).
 */
public data class TemporalInterval(
    public val startInclusive: Instant,
    public val endExclusive: Instant,
)
