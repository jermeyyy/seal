package com.jermey.seal.core.asn1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class OidTest {

    // ── fromDotNotation ─────────────────────────────────────────────

    @Test
    fun parseSimpleOid() {
        val oid = Oid.fromDotNotation("1.2.3")
        assertEquals(listOf(1, 2, 3), oid.components)
        assertEquals("1.2.3", oid.dotNotation)
    }

    @Test
    fun parseRsaOid() {
        // rsaEncryption
        val oid = Oid.fromDotNotation("1.2.840.113549.1.1.1")
        assertEquals(listOf(1, 2, 840, 113549, 1, 1, 1), oid.components)
    }

    @Test
    fun parseSha256Oid() {
        // SHA-256
        val oid = Oid.fromDotNotation("2.16.840.1.101.3.4.2.1")
        assertEquals(listOf(2, 16, 840, 1, 101, 3, 4, 2, 1), oid.components)
    }

    @Test
    fun toStringReturnsDotNotation() {
        val oid = Oid.fromDotNotation("1.3.6.1.4.1")
        assertEquals("1.3.6.1.4.1", oid.toString())
    }

    @Test
    fun blankStringThrows() {
        assertFailsWith<Asn1ParseException> { Oid.fromDotNotation("") }
        assertFailsWith<Asn1ParseException> { Oid.fromDotNotation("  ") }
    }

    @Test
    fun singleComponentThrows() {
        assertFailsWith<Asn1ParseException> { Oid.fromDotNotation("1") }
    }

    @Test
    fun nonNumericComponentThrows() {
        assertFailsWith<Asn1ParseException> { Oid.fromDotNotation("1.2.abc") }
    }

    // ── fromDer ─────────────────────────────────────────────────────

    /** Convert hex string to ByteArray. */
    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    @Test
    fun decodeSha256WithRsaEncryption() {
        // OID 1.2.840.113549.1.1.11
        // DER value bytes: 2A 86 48 86 F7 0D 01 01 0B
        val bytes = hex("2A 86 48 86 F7 0D 01 01 0B")
        val oid = Oid.fromDer(bytes)
        assertEquals("1.2.840.113549.1.1.11", oid.dotNotation)
    }

    @Test
    fun decodeRsaEncryption() {
        // OID 1.2.840.113549.1.1.1
        // First byte: 1*40+2 = 42 = 0x2A
        // 840 → base-128: 86 48
        // 113549 → base-128: 86 F7 0D
        val bytes = hex("2A 86 48 86 F7 0D 01 01 01")
        val oid = Oid.fromDer(bytes)
        assertEquals("1.2.840.113549.1.1.1", oid.dotNotation)
    }

    @Test
    fun decodeSha256() {
        // OID 2.16.840.1.101.3.4.2.1
        // First byte: 2*40+16 = 96 = 0x60
        // 840 → 86 48, then 01, 65, 03, 04, 02, 01
        val bytes = hex("60 86 48 01 65 03 04 02 01")
        val oid = Oid.fromDer(bytes)
        assertEquals("2.16.840.1.101.3.4.2.1", oid.dotNotation)
    }

    @Test
    fun decodeSimpleOid() {
        // OID 1.2.3 → first byte = 1*40+2=42=0x2A, then 0x03
        val bytes = hex("2A 03")
        val oid = Oid.fromDer(bytes)
        assertEquals("1.2.3", oid.dotNotation)
    }

    @Test
    fun decodeJointIsoItuOid() {
        // OID 2.5.4.3 (commonName) → first byte = 2*40+5=85=0x55, then 04, 03
        val bytes = hex("55 04 03")
        val oid = Oid.fromDer(bytes)
        assertEquals("2.5.4.3", oid.dotNotation)
    }

    @Test
    fun emptyBytesThrows() {
        assertFailsWith<Asn1ParseException> { Oid.fromDer(ByteArray(0)) }
    }

    // ── equals / hashCode ───────────────────────────────────────────

    @Test
    fun equalityByComponents() {
        val a = Oid.fromDotNotation("1.2.840.113549")
        val b = Oid.fromDotNotation("1.2.840.113549")
        assertEquals(a, b)
        assertEquals(a.hashCode(), b.hashCode())
    }

    // ── Roundtrip via DER → dotNotation ─────────────────────────────

    @Test
    fun derAndDotNotationProduceSameComponents() {
        // 1.2.840.113549.1.1.11 from DER bytes
        val fromDer = Oid.fromDer(hex("2A 86 48 86 F7 0D 01 01 0B"))
        val fromDot = Oid.fromDotNotation("1.2.840.113549.1.1.11")
        assertEquals(fromDot, fromDer)
    }
}
