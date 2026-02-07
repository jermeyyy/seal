package com.jermey.seal.core.loglist

import com.jermey.seal.core.crypto.CryptoVerifier
import com.jermey.seal.core.model.SignatureAlgorithm

/**
 * Verifies the digital signature of a CT log list JSON file.
 * The log list is signed by Google using ECDSA with the key in [GoogleLogListPublicKey].
 */
public class LogListSignatureVerifier(
    private val cryptoVerifier: CryptoVerifier,
) {

    /**
     * Verify the signature of a log list JSON.
     *
     * @param jsonBytes The raw JSON bytes of the log list
     * @param signatureBytes The signature bytes (from the .sig file)
     * @return true if the signature is valid
     */
    public fun verify(jsonBytes: ByteArray, signatureBytes: ByteArray): Boolean {
        return try {
            cryptoVerifier.verifySignature(
                publicKeyBytes = GoogleLogListPublicKey.keyBytes,
                data = jsonBytes,
                signature = signatureBytes,
                algorithm = SignatureAlgorithm.ECDSA,
            )
        } catch (_: Exception) {
            false
        }
    }
}
