package com.jermey.seal.core.crypto

import com.jermey.seal.core.model.SignatureAlgorithm

/**
 * Interface for cryptographic operations needed by the CT verification engine.
 * Platform-specific implementations are provided via [createCryptoVerifier].
 */
public interface CryptoVerifier {
    /**
     * Verify a digital signature.
     * @param publicKeyBytes DER-encoded SubjectPublicKeyInfo
     * @param data The signed data
     * @param signature The signature bytes
     * @param algorithm Signature algorithm (ECDSA or RSA)
     * @return true if the signature is valid
     */
    public fun verifySignature(
        publicKeyBytes: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm,
    ): Boolean

    /**
     * Compute a SHA-256 hash.
     * @param data The data to hash
     * @return The 32-byte SHA-256 hash
     */
    public fun sha256(data: ByteArray): ByteArray
}
