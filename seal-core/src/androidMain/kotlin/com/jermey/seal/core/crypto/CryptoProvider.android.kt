package com.jermey.seal.core.crypto

import com.jermey.seal.core.model.SignatureAlgorithm
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec

/**
 * Android/JVM actual implementation of [createCryptoVerifier].
 */
public actual fun createCryptoVerifier(): CryptoVerifier = JvmCryptoVerifier()

/**
 * JVM-based [CryptoVerifier] using `java.security` APIs.
 */
internal class JvmCryptoVerifier : CryptoVerifier {

    override fun verifySignature(
        publicKeyBytes: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm,
    ): Boolean = try {
        val (keyAlgorithm, signatureAlgorithm) = when (algorithm) {
            SignatureAlgorithm.ECDSA -> "EC" to "SHA256withECDSA"
            SignatureAlgorithm.RSA -> "RSA" to "SHA256withRSA"
            else -> return false
        }

        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance(keyAlgorithm)
        val publicKey = keyFactory.generatePublic(keySpec)

        val sig = Signature.getInstance(signatureAlgorithm)
        sig.initVerify(publicKey)
        sig.update(data)
        sig.verify(signature)
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        false
    }

    override fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)
}
