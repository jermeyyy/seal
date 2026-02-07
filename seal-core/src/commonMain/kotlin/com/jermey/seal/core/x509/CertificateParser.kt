package com.jermey.seal.core.x509

import com.jermey.seal.core.asn1.Asn1Element
import com.jermey.seal.core.asn1.Asn1ParseException
import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.asn1.Asn1Tag
import com.jermey.seal.core.asn1.Oid
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.parser.SctListParser

/**
 * Represents a parsed X.509 extension.
 */
public class CertificateExtension(
    /** The extension OID. */
    public val oid: Oid,
    /** Whether this extension is marked critical. */
    public val critical: Boolean,
    /** The raw extension value bytes (content of the OCTET STRING). */
    public val value: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CertificateExtension) return false
        return oid == other.oid &&
            critical == other.critical &&
            value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = oid.hashCode()
        result = 31 * result + critical.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }

    override fun toString(): String =
        "CertificateExtension(oid=$oid, critical=$critical, valueLen=${value.size})"
}

/**
 * Represents a parsed X.509 certificate with easy access to CT-relevant parts.
 */
public class ParsedCertificate(
    /** The complete certificate ASN.1 element. */
    public val certificateElement: Asn1Element,
    /** The TBS (To Be Signed) certificate ASN.1 element. */
    public val tbsCertificate: Asn1Element,
    /** The parsed extensions, if present. */
    public val extensions: List<CertificateExtension>,
)

/**
 * Parses X.509 certificates to extract CT-relevant information.
 *
 * Supports extracting embedded SCTs, detecting precertificates,
 * and reading SubjectPublicKeyInfo.
 */
public object CertificateParser {

    /** Context-specific tag [3] constructed — wraps the extensions field in TBSCertificate. */
    private val EXTENSIONS_TAG = Asn1Tag.contextSpecific(3, constructed = true)

    /** Context-specific tag [0] constructed — wraps the version field in TBSCertificate. */
    private val VERSION_TAG = Asn1Tag.contextSpecific(0, constructed = true)

    /**
     * Parse an X.509 certificate from DER bytes and extract the TBS certificate element,
     * extensions, and signature.
     *
     * @param derBytes The complete certificate DER encoding.
     * @return A [ParsedCertificate] with the parsed structure.
     * @throws Asn1ParseException if the certificate structure is invalid.
     */
    public fun parseCertificate(derBytes: ByteArray): ParsedCertificate {
        val cert = Asn1Parser.parse(derBytes)
        if (cert.tag != Asn1Tag.SEQUENCE) {
            throw Asn1ParseException("Certificate must be a SEQUENCE, got ${cert.tag}")
        }

        val tbs = cert.childAt(0)
            ?: throw Asn1ParseException("Certificate SEQUENCE is empty — missing TBSCertificate")
        if (tbs.tag != Asn1Tag.SEQUENCE) {
            throw Asn1ParseException("TBSCertificate must be a SEQUENCE, got ${tbs.tag}")
        }

        val extensions = parseExtensions(tbs)
        return ParsedCertificate(cert, tbs, extensions)
    }

    /**
     * Extract embedded SCTs from a certificate's SCT extension.
     *
     * Returns an empty list if no SCT extension is found or if the SCT data cannot be parsed.
     *
     * @param derBytes The complete certificate DER encoding.
     * @return The list of parsed [SignedCertificateTimestamp]s.
     */
    public fun extractEmbeddedScts(derBytes: ByteArray): List<SignedCertificateTimestamp> {
        val parsed = parseCertificate(derBytes)
        val sctExt = parsed.extensions.firstOrNull {
            it.oid == CertificateExtensions.SCT_EXTENSION_OID
        } ?: return emptyList()

        // The extension value is an OCTET STRING wrapping the SCT list.
        // Parse the inner OCTET STRING to get the actual SCT list bytes.
        return try {
            val inner = Asn1Parser.parse(sctExt.value)
            if (inner.tag == Asn1Tag.OCTET_STRING) {
                SctListParser.parse(inner.rawValue, Origin.EMBEDDED)
            } else {
                // Try parsing directly if there's no inner OCTET STRING wrapper
                SctListParser.parse(sctExt.value, Origin.EMBEDDED)
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /**
     * Check if a certificate is a precertificate (has CT poison extension).
     *
     * @param derBytes The complete certificate DER encoding.
     * @return `true` if the certificate contains the precertificate poison extension.
     */
    public fun isPrecertificate(derBytes: ByteArray): Boolean {
        val parsed = parseCertificate(derBytes)
        return parsed.extensions.any { it.oid == CertificateExtensions.PRECERT_POISON_OID }
    }

    /**
     * Extract the SubjectPublicKeyInfo bytes from a certificate.
     *
     * In the TBSCertificate, the SPKI is located after the version (optional), serialNumber,
     * signature algorithm, issuer, validity, and subject fields.
     *
     * @param derBytes The complete certificate DER encoding.
     * @return The full DER encoding of the SubjectPublicKeyInfo element.
     * @throws Asn1ParseException if the SPKI cannot be found.
     */
    public fun extractSubjectPublicKeyInfo(derBytes: ByteArray): ByteArray {
        val parsed = parseCertificate(derBytes)
        val tbs = parsed.tbsCertificate

        // Determine the index offset based on the presence of the explicit version tag [0].
        val hasVersion = tbs.childAt(0)?.tag == VERSION_TAG
        // TBSCertificate fields:
        //   [0] version (optional), serialNumber, signatureAlg, issuer, validity, subject, SPKI
        // SPKI index: 6 if version present, 5 if absent.
        val spkiIndex = if (hasVersion) 6 else 5

        val spki = tbs.childAt(spkiIndex)
            ?: throw Asn1ParseException(
                "SubjectPublicKeyInfo not found at index $spkiIndex in TBSCertificate"
            )

        return spki.fullEncoding
    }

    // ── Internal helpers ────────────────────────────────────────────

    /**
     * Parse extensions from a TBSCertificate element.
     */
    internal fun parseExtensions(tbs: Asn1Element): List<CertificateExtension> {
        // Find the extensions wrapper: context-specific [3] constructed
        val extensionsWrapper = tbs.findChild(EXTENSIONS_TAG) ?: return emptyList()

        // Inside [3] there's a SEQUENCE of Extension
        val extensionsSeq = extensionsWrapper.findChild(Asn1Tag.SEQUENCE) ?: return emptyList()

        return extensionsSeq.children.mapNotNull { extElement ->
            parseExtension(extElement)
        }
    }

    /**
     * Parse a single Extension element.
     *
     * Extension ::= SEQUENCE { OID, [optional BOOLEAN], OCTET STRING }
     */
    private fun parseExtension(element: Asn1Element): CertificateExtension? {
        if (element.tag != Asn1Tag.SEQUENCE) return null
        if (element.children.isEmpty()) return null

        val oidElement = element.childAt(0) ?: return null
        if (oidElement.tag != Asn1Tag.OID) return null
        val oid = Oid.fromDer(oidElement.rawValue)

        var critical = false
        var valueElement: Asn1Element? = null

        when (element.children.size) {
            2 -> {
                // OID + OCTET STRING (critical defaults to false)
                valueElement = element.childAt(1)
            }
            3 -> {
                // OID + BOOLEAN (critical) + OCTET STRING
                val boolElement = element.childAt(1)
                if (boolElement != null && boolElement.tag == Asn1Tag.BOOLEAN) {
                    critical = boolElement.rawValue.isNotEmpty() && boolElement.rawValue[0] != 0.toByte()
                }
                valueElement = element.childAt(2)
            }
            else -> return null
        }

        if (valueElement == null || valueElement.tag != Asn1Tag.OCTET_STRING) return null

        return CertificateExtension(oid, critical, valueElement.rawValue)
    }
}
