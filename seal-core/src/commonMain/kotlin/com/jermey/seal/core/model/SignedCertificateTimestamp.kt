package com.jermey.seal.core.model

import kotlinx.datetime.Instant

/**
 * A Signed Certificate Timestamp (SCT) as defined in RFC 6962 §3.2.
 *
 * An SCT is a promise from a CT log that a certificate (or precertificate) has been
 * logged. It contains the log's identity, a timestamp, and a digital signature that
 * can be verified against the log's public key.
 *
 * SCTs can be delivered via three mechanisms, indicated by [origin]:
 * - Embedded in the certificate's X.509 extensions
 * - Via TLS extension during the handshake
 * - Via OCSP stapled response
 *
 * @property version The SCT structure version (currently only [SctVersion.V1]).
 * @property logId The identity of the log that issued this SCT.
 * @property timestamp When the log recorded (or promised to record) the certificate.
 * @property extensions Optional SCT extension bytes (currently unused in RFC 6962 v1).
 * @property signature The log's digital signature over the SCT data.
 * @property origin How this SCT was delivered to the client.
 * @see SctVersion
 * @see DigitallySigned
 */
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

/**
 * Describes how a [SignedCertificateTimestamp] was delivered to the TLS client.
 */
public enum class Origin {
    /** SCT was embedded in the leaf certificate's X.509v3 extensions. */
    EMBEDDED,

    /** SCT was delivered via the TLS `signed_certificate_timestamp` extension. */
    TLS_EXTENSION,

    /** SCT was delivered inside an OCSP stapled response. */
    OCSP_RESPONSE,
}
