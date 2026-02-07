package com.jermey.seal.core.verification

import com.jermey.seal.core.crypto.CryptoVerifier
import com.jermey.seal.core.loglist.LogListResult
import com.jermey.seal.core.loglist.LogListService
import com.jermey.seal.core.model.*
import com.jermey.seal.core.parser.SctListParser
import com.jermey.seal.core.policy.CTPolicy
import com.jermey.seal.core.x509.CertificateParser
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

/**
 * Main CT verification engine that orchestrates the verification process.
 *
 * Ties together SCT extraction, signature verification, log list lookup,
 * and policy evaluation.
 *
 * @param cryptoVerifier Platform-specific crypto implementation
 * @param logListService Service for obtaining the trusted log list
 * @param policy CT policy to evaluate (e.g., Chrome or Apple policy)
 */
public class CertificateTransparencyVerifier(
    private val cryptoVerifier: CryptoVerifier,
    private val logListService: LogListService,
    private val policy: CTPolicy,
) {
    private val sctSignatureVerifier = SctSignatureVerifier(cryptoVerifier)

    /**
     * Verify certificate transparency compliance for a certificate chain.
     *
     * @param certificateChain The certificate chain as a list of DER-encoded certificates.
     *        The first element is the leaf certificate, the second is the issuer, etc.
     * @param tlsExtensionSctBytes Optional SCT bytes from the TLS extension (RFC 6962 §3.3).
     * @param ocspResponseSctBytes Optional SCT bytes from an OCSP response (RFC 6962 §3.3).
     * @return VerificationResult indicating whether the chain is CT-compliant.
     */
    public suspend fun verify(
        certificateChain: List<ByteArray>,
        tlsExtensionSctBytes: ByteArray? = null,
        ocspResponseSctBytes: ByteArray? = null,
    ): VerificationResult {
        if (certificateChain.isEmpty()) {
            return VerificationResult.Failure.NoScts
        }

        return try {
            val leafCertDer = certificateChain[0]
            val issuerCertDer = certificateChain.getOrNull(1)

            // Step 1: Collect all SCTs from all sources
            val allScts = collectScts(leafCertDer, tlsExtensionSctBytes, ocspResponseSctBytes)

            if (allScts.isEmpty()) {
                return VerificationResult.Failure.NoScts
            }

            // Step 2: Get the trusted log list
            val logListResult = logListService.getLogList()
            val logServers = when (logListResult) {
                is LogListResult.Success -> {
                    if (logListResult.isStale) {
                        // If log list is stale but we still have servers, proceed with warning
                        // but if user configured fail-open this will still work
                    }
                    logListResult.servers
                }
                is LogListResult.Failure -> {
                    return VerificationResult.Failure.UnknownError(
                        logListResult.error
                    )
                }
            }

            // Build a lookup map: LogId -> LogServer
            val logServersByLogId = logServers.associateBy { it.logId }

            // Step 3: Verify each SCT against its log
            val sctResults = allScts.map { sct ->
                verifySct(sct, logServersByLogId, leafCertDer, issuerCertDer)
            }

            // Step 4: Calculate certificate lifetime
            val certificateLifetimeDays = calculateCertificateLifetimeDays(leafCertDer)

            // Step 5: Apply policy
            policy.evaluate(certificateLifetimeDays, sctResults)
        } catch (e: Exception) {
            VerificationResult.Failure.UnknownError(e)
        }
    }

    /**
     * Collect SCTs from all available sources.
     */
    private fun collectScts(
        leafCertDer: ByteArray,
        tlsExtensionSctBytes: ByteArray?,
        ocspResponseSctBytes: ByteArray?,
    ): List<SignedCertificateTimestamp> {
        val allScts = mutableListOf<SignedCertificateTimestamp>()

        // 1. Extract embedded SCTs from the leaf certificate
        try {
            val embeddedScts = CertificateParser.extractEmbeddedScts(leafCertDer)
            allScts.addAll(embeddedScts)
        } catch (_: Exception) {
            // Skip if extraction fails
        }

        // 2. Parse TLS extension SCTs
        if (tlsExtensionSctBytes != null && tlsExtensionSctBytes.isNotEmpty()) {
            try {
                val tlsScts = SctListParser.parse(tlsExtensionSctBytes, Origin.TLS_EXTENSION)
                allScts.addAll(tlsScts)
            } catch (_: Exception) {
                // Skip if parsing fails
            }
        }

        // 3. Parse OCSP response SCTs
        if (ocspResponseSctBytes != null && ocspResponseSctBytes.isNotEmpty()) {
            try {
                val ocspScts = SctListParser.parse(ocspResponseSctBytes, Origin.OCSP_RESPONSE)
                allScts.addAll(ocspScts)
            } catch (_: Exception) {
                // Skip if parsing fails
            }
        }

        return allScts
    }

    /**
     * Verify a single SCT against the trusted log list.
     */
    private fun verifySct(
        sct: SignedCertificateTimestamp,
        logServersByLogId: Map<LogId, LogServer>,
        leafCertDer: ByteArray,
        issuerCertDer: ByteArray?,
    ): SctVerificationResult {
        val logServer = logServersByLogId[sct.logId]
            ?: return SctVerificationResult.Invalid.LogNotTrusted(sct)

        return sctSignatureVerifier.verify(sct, logServer, leafCertDer, issuerCertDer)
    }

    /**
     * Calculate the certificate's validity period in days.
     * Parses the notBefore and notAfter dates from the TBSCertificate.
     */
    private fun calculateCertificateLifetimeDays(leafCertDer: ByteArray): Long {
        return try {
            val parsed = CertificateParser.parseCertificate(leafCertDer)
            val tbs = parsed.tbsCertificate

            // TBSCertificate fields:
            // [0] version (optional), serialNumber, signatureAlg, issuer, validity, subject, SPKI
            val hasVersion = tbs.childAt(0)?.let {
                it.tag == com.jermey.seal.core.asn1.Asn1Tag.contextSpecific(0, constructed = true)
            } ?: false

            val validityIndex = if (hasVersion) 4 else 3
            val validityElement = tbs.childAt(validityIndex)

            if (validityElement != null && validityElement.children.size >= 2) {
                val notBeforeBytes = validityElement.childAt(0)?.rawValue
                val notAfterBytes = validityElement.childAt(1)?.rawValue
                if (notBeforeBytes != null && notAfterBytes != null) {
                    val notBefore = parseAsn1Time(notBeforeBytes)
                    val notAfter = parseAsn1Time(notAfterBytes)
                    val durationMs = notAfter - notBefore
                    (durationMs / (24 * 60 * 60 * 1000L)).coerceAtLeast(0)
                } else {
                    397L // Default if validity bytes are missing
                }
            } else {
                397L // Default to a reasonable value (~13 months)
            }
        } catch (_: Exception) {
            397L // Default if parsing fails
        }
    }

    /**
     * Parse ASN.1 UTCTime or GeneralizedTime to epoch milliseconds.
     */
    private fun parseAsn1Time(bytes: ByteArray): Long {
        val timeStr = bytes.decodeToString()
        return try {
            if (timeStr.length <= 13) {
                // UTCTime: YYMMDDHHMMSSZ
                parseUtcTime(timeStr)
            } else {
                // GeneralizedTime: YYYYMMDDHHMMSSZ
                parseGeneralizedTime(timeStr)
            }
        } catch (_: Exception) {
            0L
        }
    }

    private fun parseUtcTime(s: String): Long {
        val clean = s.trimEnd('Z')
        if (clean.length < 12) return 0L
        var year = clean.substring(0, 2).toInt()
        year += if (year >= 50) 1900 else 2000
        val month = clean.substring(2, 4).toInt()
        val day = clean.substring(4, 6).toInt()
        val hour = clean.substring(6, 8).toInt()
        val minute = clean.substring(8, 10).toInt()
        val second = clean.substring(10, 12).toInt()
        return toEpochMillis(year, month, day, hour, minute, second)
    }

    private fun parseGeneralizedTime(s: String): Long {
        val clean = s.trimEnd('Z')
        if (clean.length < 14) return 0L
        val year = clean.substring(0, 4).toInt()
        val month = clean.substring(4, 6).toInt()
        val day = clean.substring(6, 8).toInt()
        val hour = clean.substring(8, 10).toInt()
        val minute = clean.substring(10, 12).toInt()
        val second = clean.substring(12, 14).toInt()
        return toEpochMillis(year, month, day, hour, minute, second)
    }

    /**
     * Simple epoch millis calculation (approximate, sufficient for lifetime calculation).
     */
    private fun toEpochMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int, second: Int): Long {
        // Use kotlinx-datetime for proper calculation
        try {
            val instant = LocalDateTime(year, month, day, hour, minute, second)
                .toInstant(TimeZone.UTC)
            return instant.toEpochMilliseconds()
        } catch (_: Exception) {
            return 0L
        }
    }
}
