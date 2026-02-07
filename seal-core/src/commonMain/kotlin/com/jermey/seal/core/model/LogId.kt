package com.jermey.seal.core.model

/**
 * Represents a CT Log ID — the SHA-256 hash of the log's public key (32 bytes).
 */
public data class LogId(val keyId: ByteArray) {
    init {
        require(keyId.size == 32) { "LogId must be exactly 32 bytes, got ${keyId.size}" }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LogId) return false
        return keyId.contentEquals(other.keyId)
    }

    override fun hashCode(): Int = keyId.contentHashCode()

    override fun toString(): String = "LogId(${keyId.joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }})"
}
