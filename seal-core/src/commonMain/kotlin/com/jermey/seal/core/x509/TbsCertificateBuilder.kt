package com.jermey.seal.core.x509

import com.jermey.seal.core.asn1.Asn1Element
import com.jermey.seal.core.asn1.Asn1ParseException
import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.asn1.Asn1Tag
import com.jermey.seal.core.asn1.Oid

/**
 * Reconstructs the TBS certificate bytes for SCT signature verification.
 *
 * For precertificates, the poison extension is removed.
 * For regular certificates with embedded SCTs, the SCT extension is removed.
 */
public object TbsCertificateBuilder {

    /** Context-specific tag [3] constructed — the extensions wrapper. */
    private val EXTENSIONS_TAG = Asn1Tag.contextSpecific(3, constructed = true)

    /**
     * Reconstruct the TBS certificate bytes for SCT signature verification.
     *
     * For precertificates:
     * - Removes the poison extension (1.3.6.1.4.1.11129.2.4.3)
     *
     * For regular certificates with embedded SCTs:
     * - Removes the SCT extension (1.3.6.1.4.1.11129.2.4.2)
     *
     * @param certDerBytes The complete certificate DER bytes.
     * @return The reconstructed TBS certificate bytes for signature verification.
     * @throws Asn1ParseException if the certificate structure is invalid.
     */
    public fun reconstructTbsForVerification(certDerBytes: ByteArray): ByteArray {
        val cert = Asn1Parser.parse(certDerBytes)
        if (cert.tag != Asn1Tag.SEQUENCE) {
            throw Asn1ParseException("Certificate must be a SEQUENCE, got ${cert.tag}")
        }

        val tbs = cert.childAt(0)
            ?: throw Asn1ParseException("Certificate SEQUENCE is empty — missing TBSCertificate")
        if (tbs.tag != Asn1Tag.SEQUENCE) {
            throw Asn1ParseException("TBSCertificate must be a SEQUENCE, got ${tbs.tag}")
        }

        val extensionsWrapper = tbs.findChild(EXTENSIONS_TAG)
            ?: return tbs.fullEncoding // No extensions — return as-is

        val extensionsSeq = extensionsWrapper.findChild(Asn1Tag.SEQUENCE)
            ?: return tbs.fullEncoding // Malformed extensions — return as-is

        // Determine which OIDs to remove
        val oidsToRemove = setOf(
            CertificateExtensions.PRECERT_POISON_OID,
            CertificateExtensions.SCT_EXTENSION_OID,
        )

        // Filter extensions, keeping only those whose OID is NOT in the removal set
        val filteredExtChildren = extensionsSeq.children.filter { extElement ->
            if (extElement.tag != Asn1Tag.SEQUENCE) return@filter true
            val oidElement = extElement.childAt(0) ?: return@filter true
            if (oidElement.tag != Asn1Tag.OID) return@filter true
            val oid = Oid.fromDer(oidElement.rawValue)
            oid !in oidsToRemove
        }

        // Re-encode the extensions SEQUENCE
        val filteredExtSeqBytes = encodeDerSequence(filteredExtChildren.map { it.fullEncoding })

        // Re-encode the [3] wrapper around the extensions SEQUENCE
        val newExtensionsWrapper = encodeDerTagAndValue(EXTENSIONS_TAG, filteredExtSeqBytes)

        // Rebuild the TBS: replace the extensions wrapper element
        val newTbsChildren = tbs.children.map { child ->
            if (child.tag == EXTENSIONS_TAG) {
                newExtensionsWrapper
            } else {
                child.fullEncoding
            }
        }

        return encodeDerSequence(newTbsChildren)
    }
}

// ── Internal DER encoding helpers ───────────────────────────────────

/**
 * Encode a DER length using definite form.
 */
internal fun encodeDerLength(length: Int): ByteArray {
    return when {
        length < 0x80 -> byteArrayOf(length.toByte())
        length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
        length <= 0xFFFF -> byteArrayOf(
            0x82.toByte(),
            (length shr 8).toByte(),
            (length and 0xFF).toByte(),
        )
        length <= 0xFFFFFF -> byteArrayOf(
            0x83.toByte(),
            (length shr 16).toByte(),
            ((length shr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte(),
        )
        else -> byteArrayOf(
            0x84.toByte(),
            (length shr 24).toByte(),
            ((length shr 16) and 0xFF).toByte(),
            ((length shr 8) and 0xFF).toByte(),
            (length and 0xFF).toByte(),
        )
    }
}

/**
 * Encode a DER SEQUENCE (tag 0x30) wrapping the concatenation of [children] byte arrays.
 */
internal fun encodeDerSequence(children: List<ByteArray>): ByteArray {
    val content = concatenate(children)
    val tag = byteArrayOf(0x30)
    val length = encodeDerLength(content.size)
    return tag + length + content
}

/**
 * Encode a DER element with the given [tag] and [value] bytes.
 */
internal fun encodeDerTagAndValue(tag: Asn1Tag, value: ByteArray): ByteArray {
    val tagBytes = encodeTag(tag)
    val lengthBytes = encodeDerLength(value.size)
    return tagBytes + lengthBytes + value
}

/**
 * Encode an ASN.1 tag into its DER byte representation.
 */
private fun encodeTag(tag: Asn1Tag): ByteArray {
    val classBits = tag.tagClass.mask
    val constructedBit = if (tag.isConstructed) 0x20 else 0x00

    return if (tag.tagNumber < 0x1F) {
        // Short form
        byteArrayOf((classBits or constructedBit or tag.tagNumber).toByte())
    } else {
        // Long form
        val firstByte = (classBits or constructedBit or 0x1F).toByte()
        val numberBytes = encodeBase128(tag.tagNumber)
        byteArrayOf(firstByte) + numberBytes
    }
}

/**
 * Encode an integer in base-128 with high-bit continuation (for long-form tag numbers).
 */
private fun encodeBase128(value: Int): ByteArray {
    if (value < 0x80) return byteArrayOf(value.toByte())

    val bytes = mutableListOf<Byte>()
    var remaining = value
    bytes.add((remaining and 0x7F).toByte())
    remaining = remaining shr 7
    while (remaining > 0) {
        bytes.add(((remaining and 0x7F) or 0x80).toByte())
        remaining = remaining shr 7
    }
    bytes.reverse()
    return bytes.toByteArray()
}

/**
 * Concatenate a list of byte arrays into a single byte array.
 */
private fun concatenate(arrays: List<ByteArray>): ByteArray {
    val totalSize = arrays.sumOf { it.size }
    val result = ByteArray(totalSize)
    var offset = 0
    for (array in arrays) {
        array.copyInto(result, offset)
        offset += array.size
    }
    return result
}
