package com.jermey.seal.core.x509

import com.jermey.seal.core.asn1.Asn1ParseException
import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.asn1.Asn1Tag
import com.jermey.seal.core.asn1.Oid
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CertificateParserTest {

    // ── DER building helpers ────────────────────────────────────────

    /** Encode a DER length. */
    private fun derLength(length: Int): ByteArray = when {
        length < 0x80 -> byteArrayOf(length.toByte())
        length <= 0xFF -> byteArrayOf(0x81.toByte(), length.toByte())
        length <= 0xFFFF -> byteArrayOf(
            0x82.toByte(),
            (length shr 8).toByte(),
            (length and 0xFF).toByte(),
        )
        else -> error("Length too large for test helper")
    }

    /** Build a DER SEQUENCE from child byte arrays. */
    private fun buildSequence(vararg children: ByteArray): ByteArray {
        val content = children.fold(byteArrayOf()) { acc, b -> acc + b }
        return byteArrayOf(0x30) + derLength(content.size) + content
    }

    /** Build a DER context-specific constructed tagged element [tagNumber]. */
    private fun buildContextConstructed(tagNumber: Int, content: ByteArray): ByteArray {
        val tagByte = (0xA0 or tagNumber).toByte() // context-specific + constructed
        return byteArrayOf(tagByte) + derLength(content.size) + content
    }

    /** Build a DER OCTET STRING. */
    private fun buildOctetString(content: ByteArray): ByteArray {
        return byteArrayOf(0x04) + derLength(content.size) + content
    }

    /** Build a DER INTEGER with a single-byte value. */
    private fun buildInteger(value: Int): ByteArray {
        return byteArrayOf(0x02, 0x01, value.toByte())
    }

    /** Build a DER BOOLEAN. */
    private fun buildBoolean(value: Boolean): ByteArray {
        return byteArrayOf(0x01, 0x01, if (value) 0xFF.toByte() else 0x00)
    }

    /** Build a DER BIT STRING with zero unused bits. */
    private fun buildBitString(content: ByteArray): ByteArray {
        val value = byteArrayOf(0x00) + content // 0 unused bits
        return byteArrayOf(0x03) + derLength(value.size) + value
    }

    /** Build a DER NULL. */
    private fun buildNull(): ByteArray = byteArrayOf(0x05, 0x00)

    /** Encode an OID value in DER (content bytes only). */
    private fun encodeOidValue(dotNotation: String): ByteArray {
        val parts = dotNotation.split(".").map { it.toInt() }
        require(parts.size >= 2)
        val result = mutableListOf<Byte>()
        // First two components
        result.add((parts[0] * 40 + parts[1]).toByte())
        // Subsequent components in base-128
        for (i in 2 until parts.size) {
            val component = parts[i]
            if (component < 0x80) {
                result.add(component.toByte())
            } else {
                val bytes = mutableListOf<Byte>()
                var remaining = component
                bytes.add((remaining and 0x7F).toByte())
                remaining = remaining shr 7
                while (remaining > 0) {
                    bytes.add(((remaining and 0x7F) or 0x80).toByte())
                    remaining = remaining shr 7
                }
                bytes.reverse()
                result.addAll(bytes)
            }
        }
        return result.toByteArray()
    }

    /** Build a DER OID element from dot notation. */
    private fun buildOid(dotNotation: String): ByteArray {
        val value = encodeOidValue(dotNotation)
        return byteArrayOf(0x06) + derLength(value.size) + value
    }

    /** Build a DER signature algorithm (SEQUENCE { OID, NULL }). */
    private fun buildSigAlg(): ByteArray =
        buildSequence(buildOid("1.2.840.113549.1.1.11"), buildNull())

    /** Build a minimal DER Name (SEQUENCE { SET { SEQUENCE { OID, UTF8String } } }). */
    private fun buildName(): ByteArray {
        val cn = byteArrayOf(0x0C, 0x04) + "test".encodeToByteArray() // UTF8String "test"
        val atv = buildSequence(buildOid("2.5.4.3"), cn) // CommonName
        val rdn = byteArrayOf(0x31) + derLength(atv.size) + atv // SET
        return buildSequence(rdn)
    }

    /** Build a minimal DER Validity. */
    private fun buildValidity(): ByteArray {
        val time = "250101000000Z".encodeToByteArray()
        val utc = byteArrayOf(0x17) + derLength(time.size) + time
        return buildSequence(utc, utc)
    }

    /** Build a minimal SubjectPublicKeyInfo. */
    private fun buildSpki(): ByteArray {
        val alg = buildSigAlg()
        val keyBits = buildBitString(byteArrayOf(0x00, 0x01, 0x02, 0x03))
        return buildSequence(alg, keyBits)
    }

    /** Build an X.509 Extension SEQUENCE { OID, [BOOLEAN], OCTET STRING }. */
    private fun buildExtension(
        oidDotNotation: String,
        critical: Boolean = false,
        value: ByteArray,
    ): ByteArray {
        val elements = mutableListOf(buildOid(oidDotNotation))
        if (critical) {
            elements.add(buildBoolean(true))
        }
        elements.add(buildOctetString(value))
        return buildSequence(*elements.toTypedArray())
    }

    /**
     * Build a minimal TBSCertificate with the given extensions.
     * Returns the TBS SEQUENCE bytes.
     */
    private fun buildTbs(extensions: List<ByteArray> = emptyList()): ByteArray {
        val version = buildContextConstructed(0, buildInteger(2)) // v3
        val serial = buildInteger(1)
        val sigAlg = buildSigAlg()
        val issuer = buildName()
        val validity = buildValidity()
        val subject = buildName()
        val spki = buildSpki()

        val tbsChildren = mutableListOf(
            version, serial, sigAlg, issuer, validity, subject, spki
        )

        if (extensions.isNotEmpty()) {
            val extensionsSeq = buildSequence(*extensions.toTypedArray())
            val extensionsWrapper = buildContextConstructed(3, extensionsSeq)
            tbsChildren.add(extensionsWrapper)
        }

        return buildSequence(*tbsChildren.toTypedArray())
    }

    /**
     * Build a complete X.509 certificate from a TBS, signature algorithm, and signature.
     */
    private fun buildCertificate(tbs: ByteArray): ByteArray {
        val sigAlg = buildSigAlg()
        val sig = buildBitString(byteArrayOf(0xAA.toByte(), 0xBB.toByte()))
        return buildSequence(tbs, sigAlg, sig)
    }

    /** Build a complete certificate with given extensions. */
    private fun buildCertificateWithExtensions(extensions: List<ByteArray>): ByteArray {
        val tbs = buildTbs(extensions)
        return buildCertificate(tbs)
    }

    // ── CertificateExtensions tests ─────────────────────────────────

    @Test
    fun sctExtensionOidHasCorrectValue() {
        assertEquals(
            "1.3.6.1.4.1.11129.2.4.2",
            CertificateExtensions.SCT_EXTENSION_OID.dotNotation,
        )
    }

    @Test
    fun precertPoisonOidHasCorrectValue() {
        assertEquals(
            "1.3.6.1.4.1.11129.2.4.3",
            CertificateExtensions.PRECERT_POISON_OID.dotNotation,
        )
    }

    @Test
    fun precertSigningCertOidHasCorrectValue() {
        assertEquals(
            "1.3.6.1.4.1.11129.2.4.4",
            CertificateExtensions.PRECERT_SIGNING_CERT_OID.dotNotation,
        )
    }

    // ── CertificateParser.parseCertificate tests ────────────────────

    @Test
    fun parseCertificateWithNoExtensions() {
        val cert = buildCertificateWithExtensions(emptyList())
        val parsed = CertificateParser.parseCertificate(cert)

        assertEquals(Asn1Tag.SEQUENCE, parsed.certificateElement.tag)
        assertEquals(Asn1Tag.SEQUENCE, parsed.tbsCertificate.tag)
        assertTrue(parsed.extensions.isEmpty())
    }

    @Test
    fun parseCertificateWithSingleExtension() {
        val extValue = byteArrayOf(0x01, 0x02, 0x03)
        val ext = buildExtension("2.5.29.14", value = extValue) // SubjectKeyIdentifier
        val cert = buildCertificateWithExtensions(listOf(ext))
        val parsed = CertificateParser.parseCertificate(cert)

        assertEquals(1, parsed.extensions.size)
        assertEquals("2.5.29.14", parsed.extensions[0].oid.dotNotation)
        assertFalse(parsed.extensions[0].critical)
        assertTrue(extValue.contentEquals(parsed.extensions[0].value))
    }

    @Test
    fun parseCertificateWithCriticalExtension() {
        val extValue = byteArrayOf(0xAA.toByte())
        val ext = buildExtension("2.5.29.15", critical = true, value = extValue) // KeyUsage
        val cert = buildCertificateWithExtensions(listOf(ext))
        val parsed = CertificateParser.parseCertificate(cert)

        assertEquals(1, parsed.extensions.size)
        assertTrue(parsed.extensions[0].critical)
    }

    @Test
    fun parseCertificateWithMultipleExtensions() {
        val ext1 = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val ext2 = buildExtension("2.5.29.15", critical = true, value = byteArrayOf(0x02))
        val ext3 = buildExtension("2.5.29.19", value = byteArrayOf(0x03))
        val cert = buildCertificateWithExtensions(listOf(ext1, ext2, ext3))
        val parsed = CertificateParser.parseCertificate(cert)

        assertEquals(3, parsed.extensions.size)
        assertEquals("2.5.29.14", parsed.extensions[0].oid.dotNotation)
        assertEquals("2.5.29.15", parsed.extensions[1].oid.dotNotation)
        assertEquals("2.5.29.19", parsed.extensions[2].oid.dotNotation)
    }

    @Test
    fun parseCertificateRejectsNonSequence() {
        // Just an INTEGER, not a valid certificate
        val data = buildInteger(42)
        assertFailsWith<Asn1ParseException> {
            CertificateParser.parseCertificate(data)
        }
    }

    // ── CertificateParser.isPrecertificate tests ────────────────────

    @Test
    fun isPrecertificateReturnsTrueForPoisonExtension() {
        val poison = buildExtension(
            "1.3.6.1.4.1.11129.2.4.3",
            critical = true,
            value = byteArrayOf(0x05, 0x00), // NULL
        )
        val cert = buildCertificateWithExtensions(listOf(poison))

        assertTrue(CertificateParser.isPrecertificate(cert))
    }

    @Test
    fun isPrecertificateReturnsFalseWithoutPoisonExtension() {
        val ext = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val cert = buildCertificateWithExtensions(listOf(ext))

        assertFalse(CertificateParser.isPrecertificate(cert))
    }

    @Test
    fun isPrecertificateReturnsFalseForNoExtensions() {
        val cert = buildCertificateWithExtensions(emptyList())
        assertFalse(CertificateParser.isPrecertificate(cert))
    }

    // ── CertificateParser.extractSubjectPublicKeyInfo tests ─────────

    @Test
    fun extractSubjectPublicKeyInfoReturnsCorrectBytes() {
        val cert = buildCertificateWithExtensions(emptyList())
        val spkiBytes = CertificateParser.extractSubjectPublicKeyInfo(cert)

        // Parse the SPKI and verify it's a valid SEQUENCE
        val spki = Asn1Parser.parse(spkiBytes)
        assertEquals(Asn1Tag.SEQUENCE, spki.tag)
        // Should have 2 children: AlgorithmIdentifier + BIT STRING
        assertEquals(2, spki.children.size)
    }

    @Test
    fun extractSubjectPublicKeyInfoMatchesExpected() {
        val expectedSpki = buildSpki()
        val cert = buildCertificateWithExtensions(emptyList())
        val spkiBytes = CertificateParser.extractSubjectPublicKeyInfo(cert)

        assertTrue(expectedSpki.contentEquals(spkiBytes))
    }

    // ── CertificateParser.extractEmbeddedScts tests ─────────────────

    @Test
    fun extractEmbeddedSctsReturnsEmptyWhenNoSctExtension() {
        val ext = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val cert = buildCertificateWithExtensions(listOf(ext))

        val scts = CertificateParser.extractEmbeddedScts(cert)
        assertTrue(scts.isEmpty())
    }

    @Test
    fun extractEmbeddedSctsReturnsEmptyWhenNoExtensions() {
        val cert = buildCertificateWithExtensions(emptyList())
        val scts = CertificateParser.extractEmbeddedScts(cert)
        assertTrue(scts.isEmpty())
    }

    @Test
    fun extractEmbeddedSctsHandlesMalformedSctData() {
        // SCT extension with garbage data — should return empty, not throw
        val garbage = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0xFD.toByte())
        val innerOctetString = buildOctetString(garbage)
        val sctExt = buildExtension("1.3.6.1.4.1.11129.2.4.2", value = innerOctetString)
        val cert = buildCertificateWithExtensions(listOf(sctExt))

        val scts = CertificateParser.extractEmbeddedScts(cert)
        assertTrue(scts.isEmpty())
    }

    // ── TbsCertificateBuilder tests ─────────────────────────────────

    @Test
    fun reconstructTbsRemovesPoisonExtension() {
        val poison = buildExtension(
            "1.3.6.1.4.1.11129.2.4.3",
            critical = true,
            value = byteArrayOf(0x05, 0x00),
        )
        val otherExt = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val cert = buildCertificateWithExtensions(listOf(poison, otherExt))

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)

        // Parse the reconstructed TBS and verify the poison extension is gone
        val tbs = Asn1Parser.parse(reconstructed)
        assertEquals(Asn1Tag.SEQUENCE, tbs.tag)

        val extensions = CertificateParser.parseExtensions(tbs)
        assertEquals(1, extensions.size)
        assertEquals("2.5.29.14", extensions[0].oid.dotNotation)
    }

    @Test
    fun reconstructTbsRemovesSctExtension() {
        val sctData = buildOctetString(byteArrayOf(0x00, 0x02, 0x00, 0x00))
        val sctExt = buildExtension("1.3.6.1.4.1.11129.2.4.2", value = sctData)
        val otherExt = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val cert = buildCertificateWithExtensions(listOf(sctExt, otherExt))

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)

        val tbs = Asn1Parser.parse(reconstructed)
        val extensions = CertificateParser.parseExtensions(tbs)
        assertEquals(1, extensions.size)
        assertEquals("2.5.29.14", extensions[0].oid.dotNotation)
    }

    @Test
    fun reconstructTbsRemovesBothPoisonAndSctExtensions() {
        val poison = buildExtension(
            "1.3.6.1.4.1.11129.2.4.3",
            critical = true,
            value = byteArrayOf(0x05, 0x00),
        )
        val sctData = buildOctetString(byteArrayOf(0x00, 0x02, 0x00, 0x00))
        val sctExt = buildExtension("1.3.6.1.4.1.11129.2.4.2", value = sctData)
        val otherExt = buildExtension("2.5.29.19", value = byteArrayOf(0x03))
        val cert = buildCertificateWithExtensions(listOf(poison, sctExt, otherExt))

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)

        val tbs = Asn1Parser.parse(reconstructed)
        val extensions = CertificateParser.parseExtensions(tbs)
        assertEquals(1, extensions.size)
        assertEquals("2.5.29.19", extensions[0].oid.dotNotation)
    }

    @Test
    fun reconstructTbsPreservesAllFieldsWhenNoCtExtensions() {
        val ext = buildExtension("2.5.29.14", value = byteArrayOf(0x01))
        val cert = buildCertificateWithExtensions(listOf(ext))

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)

        val tbs = Asn1Parser.parse(reconstructed)
        val extensions = CertificateParser.parseExtensions(tbs)
        assertEquals(1, extensions.size)
        assertEquals("2.5.29.14", extensions[0].oid.dotNotation)
    }

    @Test
    fun reconstructTbsHandlesNoExtensions() {
        val cert = buildCertificateWithExtensions(emptyList())
        val originalTbs = CertificateParser.parseCertificate(cert).tbsCertificate.fullEncoding

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)
        assertTrue(originalTbs.contentEquals(reconstructed))
    }

    @Test
    fun reconstructTbsProducesValidDer() {
        val poison = buildExtension(
            "1.3.6.1.4.1.11129.2.4.3",
            critical = true,
            value = byteArrayOf(0x05, 0x00),
        )
        val cert = buildCertificateWithExtensions(listOf(poison))

        val reconstructed = TbsCertificateBuilder.reconstructTbsForVerification(cert)

        // Should be parseable as valid DER
        val parsed = Asn1Parser.parse(reconstructed)
        assertEquals(Asn1Tag.SEQUENCE, parsed.tag)
        assertTrue(parsed.children.isNotEmpty())
    }

    // ── DER encoding helper tests ───────────────────────────────────

    @Test
    fun encodeDerLengthShortForm() {
        val result = encodeDerLength(0)
        assertTrue(byteArrayOf(0x00).contentEquals(result))

        val result127 = encodeDerLength(127)
        assertTrue(byteArrayOf(0x7F).contentEquals(result127))
    }

    @Test
    fun encodeDerLengthLongFormOneByte() {
        val result = encodeDerLength(128)
        assertTrue(byteArrayOf(0x81.toByte(), 0x80.toByte()).contentEquals(result))

        val result255 = encodeDerLength(255)
        assertTrue(byteArrayOf(0x81.toByte(), 0xFF.toByte()).contentEquals(result255))
    }

    @Test
    fun encodeDerLengthLongFormTwoBytes() {
        val result = encodeDerLength(256)
        assertTrue(byteArrayOf(0x82.toByte(), 0x01, 0x00).contentEquals(result))
    }

    @Test
    fun encodeDerSequenceProducesValidDer() {
        val child1 = byteArrayOf(0x02, 0x01, 0x05) // INTEGER 5
        val child2 = byteArrayOf(0x02, 0x01, 0x0A) // INTEGER 10
        val result = encodeDerSequence(listOf(child1, child2))

        val parsed = Asn1Parser.parse(result)
        assertEquals(Asn1Tag.SEQUENCE, parsed.tag)
        assertEquals(2, parsed.children.size)
    }

    // ── CertificateExtension equality tests ─────────────────────────

    @Test
    fun certificateExtensionEquality() {
        val ext1 = CertificateExtension(
            Oid.fromDotNotation("2.5.29.14"),
            false,
            byteArrayOf(0x01, 0x02),
        )
        val ext2 = CertificateExtension(
            Oid.fromDotNotation("2.5.29.14"),
            false,
            byteArrayOf(0x01, 0x02),
        )
        assertEquals(ext1, ext2)
        assertEquals(ext1.hashCode(), ext2.hashCode())
    }

    @Test
    fun certificateExtensionInequalityByOid() {
        val ext1 = CertificateExtension(
            Oid.fromDotNotation("2.5.29.14"),
            false,
            byteArrayOf(0x01),
        )
        val ext2 = CertificateExtension(
            Oid.fromDotNotation("2.5.29.15"),
            false,
            byteArrayOf(0x01),
        )
        assertFalse(ext1 == ext2)
    }
}
