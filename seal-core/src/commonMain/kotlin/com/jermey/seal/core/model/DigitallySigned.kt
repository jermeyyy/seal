package com.jermey.seal.core.model

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

public enum class HashAlgorithm(public val value: Int) {
    NONE(0), MD5(1), SHA1(2), SHA224(3), SHA256(4), SHA384(5), SHA512(6);

    public companion object {
        public fun fromValue(value: Int): HashAlgorithm? = entries.find { it.value == value }
    }
}

public enum class SignatureAlgorithm(public val value: Int) {
    ANONYMOUS(0), RSA(1), DSA(2), ECDSA(3);

    public companion object {
        public fun fromValue(value: Int): SignatureAlgorithm? = entries.find { it.value == value }
    }
}
