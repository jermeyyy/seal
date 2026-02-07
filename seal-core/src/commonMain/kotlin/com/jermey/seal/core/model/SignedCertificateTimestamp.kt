package com.jermey.seal.core.model

import kotlinx.datetime.Instant

public data class SignedCertificateTimestamp(
    public val version: SctVersion,
    public val logId: LogId,
    public val timestamp: Instant,
    public val extensions: ByteArray,
    public val signature: DigitallySigned,
    public val origin: Origin,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedCertificateTimestamp) return false
        return version == other.version &&
            logId == other.logId &&
            timestamp == other.timestamp &&
            extensions.contentEquals(other.extensions) &&
            signature == other.signature &&
            origin == other.origin
    }

    override fun hashCode(): Int {
        var result = version.hashCode()
        result = 31 * result + logId.hashCode()
        result = 31 * result + timestamp.hashCode()
        result = 31 * result + extensions.contentHashCode()
        result = 31 * result + signature.hashCode()
        result = 31 * result + origin.hashCode()
        return result
    }
}

public enum class Origin {
    EMBEDDED,
    TLS_EXTENSION,
    OCSP_RESPONSE,
}
