package com.jermey.seal.android.conscrypt

import com.jermey.seal.core.asn1.Asn1Element
import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.asn1.Asn1Tag
import com.jermey.seal.core.asn1.Oid
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.parser.SctListParser

/**
 * Parses OCSP response DER bytes to extract SCTs from the `id-pkix-ocsp-sctList` extension.
 *
 * OID: 1.3.6.1.4.1.11129.2.4.5
 *
 * OCSP response structure (simplified):
 * ```
 * OCSPResponse ::= SEQUENCE {
 *   responseStatus  ENUMERATED,
 *   responseBytes   [0] EXPLICIT SEQUENCE {  -- optional
 *     responseType  OID,
 *     response      OCTET STRING (containing BasicOCSPResponse DER)
 *   }
 * }
 *
 * BasicOCSPResponse ::= SEQUENCE {
 *   tbsResponseData    ResponseData,
 *   signatureAlgorithm AlgorithmIdentifier,
 *   signature          BIT STRING,
 *   certs              [0] EXPLICIT SEQUENCE OF Certificate OPTIONAL
 * }
 *
 * ResponseData ::= SEQUENCE {
 *   version              [0] EXPLICIT INTEGER DEFAULT v1,
 *   responderID          CHOICE { ... },
 *   producedAt           GeneralizedTime,
 *   responses            SEQUENCE OF SingleResponse,
 *   responseExtensions   [1] EXPLICIT Extensions OPTIONAL
 * }
 *
 * SingleResponse ::= SEQUENCE {
 *   certID               CertID,
 *   certStatus           CHOICE { ... },
 *   thisUpdate           GeneralizedTime,
 *   nextUpdate           [0] EXPLICIT GeneralizedTime OPTIONAL,
 *   singleExtensions     [1] EXPLICIT Extensions OPTIONAL
 * }
 * ```
 */
internal object OcspResponseParser {

    private val SCT_LIST_OID: Oid = Oid.fromDotNotation("1.3.6.1.4.1.11129.2.4.5")

    /**
     * Extract SCTs from an OCSP response's SCT extension.
     * Returns an empty list if no SCTs are found or parsing fails.
     */
    fun extractScts(ocspResponseBytes: ByteArray): List<SignedCertificateTimestamp> {
        return try {
            val ocspResponse = Asn1Parser.parse(ocspResponseBytes)
            // Navigate: OCSPResponse → responseBytes [0] → SEQUENCE → response OCTET STRING
            val responseBytes = ocspResponse.findChild(Asn1Tag.contextSpecific(0, constructed = true))
                ?: return emptyList()
            val responseBytesSeq = responseBytes.findChild(Asn1Tag.SEQUENCE)
                ?: return emptyList()
            // response is the second element (OCTET STRING) — contains BasicOCSPResponse DER
            val responseOctetString = responseBytesSeq.childAt(1)
                ?: return emptyList()

            // Parse the inner BasicOCSPResponse
            val basicOcspResponse = Asn1Parser.parse(responseOctetString.rawValue)
            // tbsResponseData is the first child SEQUENCE
            val tbsResponseData = basicOcspResponse.childAt(0)
                ?: return emptyList()

            // Collect SCTs from both response-level extensions and per-single-response extensions
            val allScts = mutableListOf<SignedCertificateTimestamp>()

            // Check response-level extensions [1]
            val responseExtensions = tbsResponseData.findChild(Asn1Tag.contextSpecific(1, constructed = true))
            if (responseExtensions != null) {
                allScts.addAll(extractSctsFromExtensions(responseExtensions))
            }

            // Check per-single-response extensions
            // Find the responses SEQUENCE (after version [0], responderID, producedAt)
            val responsesSeq = findResponsesSequence(tbsResponseData)
            if (responsesSeq != null) {
                for (singleResponse in responsesSeq.children) {
                    if (singleResponse.tag != Asn1Tag.SEQUENCE) continue
                    // singleExtensions are at [1]
                    val singleExtensions = singleResponse.findChild(Asn1Tag.contextSpecific(1, constructed = true))
                    if (singleExtensions != null) {
                        allScts.addAll(extractSctsFromExtensions(singleExtensions))
                    }
                }
            }

            allScts
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Find the responses SEQUENCE within tbsResponseData.
     * We iterate through children to find a SEQUENCE OF SingleResponse.
     */
    private fun findResponsesSequence(tbsResponseData: Asn1Element): Asn1Element? {
        // tbsResponseData children (in order):
        // [0] version (optional), responderID (CHOICE), producedAt, responses, [1] extensions (optional)
        // The responses field is a SEQUENCE whose children are also SEQUENCEs (SingleResponse)
        for (child in tbsResponseData.children) {
            if (child.tag == Asn1Tag.SEQUENCE && child.children.isNotEmpty() &&
                child.children.all { it.tag == Asn1Tag.SEQUENCE }
            ) {
                return child
            }
        }
        return null
    }

    /**
     * Extract SCTs from an Extensions container (which is wrapped in a context-specific tag).
     */
    private fun extractSctsFromExtensions(extensionsWrapper: Asn1Element): List<SignedCertificateTimestamp> {
        val extensionsSeq = extensionsWrapper.findChild(Asn1Tag.SEQUENCE)
            ?: return emptyList()

        for (extension in extensionsSeq.children) {
            if (extension.tag != Asn1Tag.SEQUENCE) continue
            val oidElement = extension.findChild(Asn1Tag.OID) ?: continue
            val oid = Oid.fromDer(oidElement.rawValue)
            if (oid == SCT_LIST_OID) {
                // The extension value is an OCTET STRING
                val valueOctetString = extension.findChild(Asn1Tag.OCTET_STRING) ?: continue
                // It may contain another OCTET STRING wrapping the SCT list
                return try {
                    val inner = Asn1Parser.parse(valueOctetString.rawValue)
                    if (inner.tag == Asn1Tag.OCTET_STRING) {
                        SctListParser.parse(inner.rawValue, Origin.OCSP_RESPONSE)
                    } else {
                        SctListParser.parse(valueOctetString.rawValue, Origin.OCSP_RESPONSE)
                    }
                } catch (_: Exception) {
                    emptyList()
                }
            }
        }
        return emptyList()
    }
}
