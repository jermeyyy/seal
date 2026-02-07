package com.jermey.seal.core.verification

import com.jermey.seal.core.crypto.CryptoVerifier
import com.jermey.seal.core.model.LogState
import com.jermey.seal.core.model.LogServer
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.x509.CertificateParser
import com.jermey.seal.core.x509.TbsCertificateBuilder

/**
 * Verifies individual SCT signatures against a log server's public key.
 * Implements the signed data reconstruction per RFC 6962 §3.2.
 */
public class SctSignatureVerifier(
    private val cryptoVerifier: CryptoVerifier,
) {
    /**
     * Verify a single SCT against a known log server.
     *
     * @param sct The SCT to verify
     * @param logServer The log server whose public key to use
     * @param leafCertDer The leaf certificate DER bytes
     * @param issuerCertDer The issuer certificate DER bytes (needed for precerts)
     * @return SctVerificationResult
     */
    public fun verify(
        sct: SignedCertificateTimestamp,
        logServer: LogServer,
        leafCertDer: ByteArray,
        issuerCertDer: ByteArray?,
    ): SctVerificationResult {
        // Check log state
        if (logServer.state == LogState.REJECTED) {
            return SctVerificationResult.Invalid.LogRejected(sct)
        }

        return try {
            // For embedded SCTs, the CT log signed over the precertificate's TBS
            // (with the SCT/poison extension removed). The final certificate won't have
            // the poison extension, so we determine the entry type from the SCT origin.
            val usePrecertEntry = when (sct.origin) {
                Origin.EMBEDDED -> true  // Embedded SCTs were signed as precert_entry
                Origin.TLS_EXTENSION -> false  // TLS SCTs use x509_entry
                Origin.OCSP_RESPONSE -> false  // OCSP SCTs use x509_entry
            }
            val signedData = buildSignedData(sct, leafCertDer, issuerCertDer, usePrecertEntry)

            println("SealCT: Verifying SCT from ${sct.origin}: usePrecertEntry=$usePrecertEntry, signedData=${signedData.size} bytes, pubKey=${logServer.publicKey.size} bytes")

            val verified = cryptoVerifier.verifySignature(
                publicKeyBytes = logServer.publicKey,
                data = signedData,
                signature = sct.signature.signature,
                algorithm = sct.signature.signatureAlgorithm,
            )

            if (verified) {
                SctVerificationResult.Valid(sct, logServer.operator)
            } else {
                SctVerificationResult.Invalid.SignatureMismatch(sct)
            }
        } catch (_: Exception) {
            SctVerificationResult.Invalid.FailedVerification(sct)
        }
    }

    /**
     * Build the digitally-signed struct per RFC 6962 §3.2.
     */
    internal fun buildSignedData(
        sct: SignedCertificateTimestamp,
        leafCertDer: ByteArray,
        issuerCertDer: ByteArray?,
        usePrecertEntry: Boolean,
    ): ByteArray {
        val buffer = mutableListOf<Byte>()

        // sct_version: 1 byte
        buffer.add(sct.version.value.toByte())

        // signature_type: 1 byte (certificate_timestamp = 0)
        buffer.add(0)

        // timestamp: 8 bytes (uint64, millis since epoch)
        val millis = sct.timestamp.toEpochMilliseconds()
        for (i in 7 downTo 0) {
            buffer.add(((millis shr (i * 8)) and 0xFF).toByte())
        }

        if (usePrecertEntry) {
            // entry_type: 2 bytes (precert_entry = 1)
            buffer.add(0)
            buffer.add(1)

            // issuer_key_hash: 32 bytes (SHA-256 of issuer's SubjectPublicKeyInfo)
            val issuerSpki = if (issuerCertDer != null) {
                CertificateParser.extractSubjectPublicKeyInfo(issuerCertDer)
            } else {
                // If no issuer provided, this is an error case but we try our best
                ByteArray(0)
            }
            val issuerKeyHash = cryptoVerifier.sha256(issuerSpki)
            buffer.addAll(issuerKeyHash.toList())

            // TBSCertificate: 3-byte length prefix + reconstructed TBS bytes
            val tbsBytes = TbsCertificateBuilder.reconstructTbsForVerification(leafCertDer)
            addWithLength24(buffer, tbsBytes)
        } else {
            // entry_type: 2 bytes (x509_entry = 0)
            buffer.add(0)
            buffer.add(0)

            // ASN.1Cert: 3-byte length prefix + cert DER bytes
            addWithLength24(buffer, leafCertDer)
        }

        // extensions: 2-byte length prefix + extension bytes
        addWithLength16(buffer, sct.extensions)

        return buffer.toByteArray()
    }

    private fun addWithLength24(buffer: MutableList<Byte>, data: ByteArray) {
        val len = data.size
        buffer.add(((len shr 16) and 0xFF).toByte())
        buffer.add(((len shr 8) and 0xFF).toByte())
        buffer.add((len and 0xFF).toByte())
        buffer.addAll(data.toList())
    }

    private fun addWithLength16(buffer: MutableList<Byte>, data: ByteArray) {
        val len = data.size
        buffer.add(((len shr 8) and 0xFF).toByte())
        buffer.add((len and 0xFF).toByte())
        buffer.addAll(data.toList())
    }
}
