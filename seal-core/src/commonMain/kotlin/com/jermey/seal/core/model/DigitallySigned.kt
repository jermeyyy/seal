package com.jermey.seal.core.model

/**
 * Represents a digitally-signed struct as defined in RFC 5246 §4.7 and used by RFC 6962.
 *
 * Contains the hash algorithm, signature algorithm, and raw signature bytes that
 * together form the cryptographic signature over an SCT's signed data.
 *
 * @property hashAlgorithm The hash algorithm used to produce the signature.
 * @property signatureAlgorithm The signature algorithm (e.g., ECDSA, RSA).
 * @property signature The raw signature bytes.
 * @see SignedCertificateTimestamp
 */
public data class DigitallySigned(
    public val hashAlgorithm: HashAlgorithm,
    public val signatureAlgorithm: SignatureAlgorithm,
    public val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DigitallySigned) return false
        return hashAlgorithm == other.hashAlgorithm &&
            signatureAlgorithm == other.signatureAlgorithm &&
            signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var result = hashAlgorithm.hashCode()
        result = 31 * result + signatureAlgorithm.hashCode()
        result = 31 * result + signature.contentHashCode()
        return result
    }
}

/**
 * Hash algorithm identifiers as defined in the TLS HashAlgorithm registry (RFC 5246 §7.4.1.4.1).
 *
 * @property value The wire-format integer value of this algorithm.
 */
public enum class HashAlgorithm(public val value: Int) {
    NONE(0), MD5(1), SHA1(2), SHA224(3), SHA256(4), SHA384(5), SHA512(6);

    public companion object {
        /**
         * Look up a [HashAlgorithm] by its wire-format [value].
         *
         * @param value The integer algorithm identifier.
         * @return The corresponding [HashAlgorithm], or `null` if the value is unknown.
         */
        public fun fromValue(value: Int): HashAlgorithm? = entries.find { it.value == value }
    }
}

/**
 * Signature algorithm identifiers as defined in the TLS SignatureAlgorithm registry (RFC 5246 §7.4.1.4.1).
 *
 * @property value The wire-format integer value of this algorithm.
 */
public enum class SignatureAlgorithm(public val value: Int) {
    ANONYMOUS(0), RSA(1), DSA(2), ECDSA(3);

    public companion object {
        /**
         * Look up a [SignatureAlgorithm] by its wire-format [value].
         *
         * @param value The integer algorithm identifier.
         * @return The corresponding [SignatureAlgorithm], or `null` if the value is unknown.
         */
        public fun fromValue(value: Int): SignatureAlgorithm? = entries.find { it.value == value }
    }
}
