package com.jermey.seal.android.conscrypt

import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.parser.SctListParser
import org.conscrypt.Conscrypt
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket

/**
 * Extracts SCTs from a Conscrypt-managed TLS connection.
 *
 * Uses Conscrypt's session APIs (via reflection, as [org.conscrypt.ConscryptSession]
 * is package-private) to access:
 * - TLS extension SCT data (`getPeerSignedCertificateTimestamp`)
 * - OCSP stapled responses (`getStatusResponses`)
 */
internal class ConscryptSctExtractor {

    /**
     * Extract SCTs delivered via the TLS extension (RFC 6962 §3.3.1).
     *
     * @param socket The TLS socket from the connection.
     * @return SCTs from the TLS extension, or empty if unavailable.
     */
    fun extractTlsExtensionScts(socket: SSLSocket): List<SignedCertificateTimestamp> {
        if (!Conscrypt.isConscrypt(socket)) return emptyList()
        return try {
            val session = socket.session ?: return emptyList()
            val rawBytes = getSctData(session) ?: return emptyList()
            if (rawBytes.isEmpty()) return emptyList()
            SctListParser.parse(rawBytes, Origin.TLS_EXTENSION)
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Extract SCTs delivered via OCSP stapling (RFC 6962 §3.3.1).
     *
     * Conscrypt may return multiple OCSP responses (one per certificate in the chain).
     * We extract SCTs from all available responses.
     *
     * @param socket The TLS socket from the connection.
     * @return SCTs from OCSP responses, or empty if unavailable.
     */
    fun extractOcspScts(socket: SSLSocket): List<SignedCertificateTimestamp> {
        if (!Conscrypt.isConscrypt(socket)) return emptyList()
        return try {
            val session = socket.session ?: return emptyList()
            // Conscrypt returns a list of OCSP responses (one per cert in chain)
            val ocspResponses = getOcspResponses(session) ?: return emptyList()
            ocspResponses.flatMap { responseBytes ->
                OcspResponseParser.extractScts(responseBytes)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Get the raw TLS extension SCT data bytes from a Conscrypt-managed socket.
     *
     * Unlike [extractTlsExtensionScts], this returns the raw byte array exactly as
     * received from the TLS extension, suitable for passing directly to
     * [com.jermey.seal.core.verification.CertificateTransparencyVerifier.verify].
     *
     * @param socket The TLS socket from the connection.
     * @return Raw SCT list bytes from the TLS extension, or null if unavailable.
     */
    fun getRawTlsSctData(socket: SSLSocket): ByteArray? {
        if (!Conscrypt.isConscrypt(socket)) return null
        return try {
            val session = socket.session ?: return null
            val rawBytes = getSctData(session)
            if (rawBytes != null && rawBytes.isNotEmpty()) rawBytes else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reflectively call `getPeerSignedCertificateTimestamp()` on the Conscrypt session.
     * This method is on the package-private `ConscryptSession` interface.
     */
    private fun getSctData(session: SSLSession): ByteArray? {
        return try {
            val method = session.javaClass.getMethod("getPeerSignedCertificateTimestamp")
            method.invoke(session) as? ByteArray
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Reflectively call `getStatusResponses()` on the Conscrypt session.
     * Returns the list of OCSP response DER bytes (one per cert in the peer chain).
     */
    @Suppress("UNCHECKED_CAST")
    private fun getOcspResponses(session: SSLSession): List<ByteArray>? {
        return try {
            val method = session.javaClass.getMethod("getStatusResponses")
            method.invoke(session) as? List<ByteArray>
        } catch (_: Exception) {
            null
        }
    }
}
