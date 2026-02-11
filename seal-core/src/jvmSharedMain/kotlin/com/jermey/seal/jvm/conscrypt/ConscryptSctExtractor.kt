package com.jermey.seal.jvm.conscrypt

import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.parser.OcspResponseParser
import com.jermey.seal.core.parser.SctListParser
import com.jermey.seal.jvm.ConscryptInitializer
import org.conscrypt.Conscrypt
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.SSLSession
import javax.net.ssl.SSLSocket

internal class ConscryptSctExtractor {

    private val logger = Logger.getLogger("SealCT")

    fun extractTlsExtensionScts(socket: SSLSocket): List<SignedCertificateTimestamp> {
        if (!ConscryptInitializer.isAvailable) return emptyList()
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

    fun extractOcspScts(socket: SSLSocket): List<SignedCertificateTimestamp> {
        if (!ConscryptInitializer.isAvailable) return emptyList()
        if (!Conscrypt.isConscrypt(socket)) return emptyList()
        return try {
            val session = socket.session ?: return emptyList()
            val ocspResponses = getOcspResponses(session) ?: return emptyList()
            ocspResponses.flatMap { responseBytes ->
                OcspResponseParser.extractScts(responseBytes)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getRawTlsSctData(socket: SSLSocket): ByteArray? {
        if (!ConscryptInitializer.isAvailable) return null
        if (!Conscrypt.isConscrypt(socket)) {
            logger.fine("Socket is not Conscrypt: ${socket.javaClass.name}")
            return null
        }
        return try {
            val session = socket.session ?: run {
                logger.fine("No SSL session available")
                return null
            }
            logger.fine("SSL session class: ${session.javaClass.name}")
            val rawBytes = getSctData(session)
            logger.fine("Raw SCT data from Conscrypt: ${rawBytes?.size ?: 0} bytes")
            if (rawBytes != null && rawBytes.isNotEmpty()) rawBytes else null
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error getting raw TLS SCT data", e)
            null
        }
    }

    fun getRawOcspResponseData(socket: SSLSocket): ByteArray? {
        if (!ConscryptInitializer.isAvailable) return null
        if (!Conscrypt.isConscrypt(socket)) return null
        return try {
            val session = socket.session ?: return null
            val ocspResponses = getOcspResponses(session) ?: return null
            val leafOcsp = ocspResponses.firstOrNull()
            logger.fine("Raw OCSP response data: ${leafOcsp?.size ?: 0} bytes")
            if (leafOcsp != null && leafOcsp.isNotEmpty()) leafOcsp else null
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error getting raw OCSP response data", e)
            null
        }
    }

    private fun getSctData(session: SSLSession): ByteArray? {
        return try {
            val method = session.javaClass.getMethod("getPeerSignedCertificateTimestamp")
            val result = method.invoke(session) as? ByteArray
            logger.fine("getPeerSignedCertificateTimestamp returned: ${result?.size ?: "null"} bytes")
            result
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Reflection call to getPeerSignedCertificateTimestamp failed", e)
            null
        }
    }

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
