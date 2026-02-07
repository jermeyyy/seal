package com.jermey.seal.core.asn1

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class Asn1ParserTest {

    // ── Helper ──────────────────────────────────────────────────────

    /** Convert hex string (e.g. "30 06 02 01 05") to ByteArray. */
    private fun hex(s: String): ByteArray =
        s.replace(" ", "").chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // ── SEQUENCE containing INTEGERs ────────────────────────────────

    @Test
    fun parseSequenceOfTwoIntegers() {
        // SEQUENCE { INTEGER 5, INTEGER 10 }
        val data = hex("30 06 02 01 05 02 01 0A")
        val seq = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.SEQUENCE, seq.tag)
        assertTrue(seq.isConstructed)
        assertEquals(2, seq.children.size)

        val first = seq.childAt(0)!!
        assertEquals(Asn1Tag.INTEGER, first.tag)
        assertEquals(1, first.rawValue.size)
        assertEquals(0x05, first.rawValue[0].toInt() and 0xFF)

        val second = seq.childAt(1)!!
        assertEquals(Asn1Tag.INTEGER, second.tag)
        assertEquals(0x0A, second.rawValue[0].toInt() and 0xFF)
    }

    // ── Primitive OCTET STRING ──────────────────────────────────────

    @Test
    fun parsePrimitiveOctetString() {
        // OCTET STRING "hello" (68 65 6C 6C 6F)
        val data = hex("04 05 68 65 6C 6C 6F")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.OCTET_STRING, element.tag)
        assertEquals("hello", element.rawValue.decodeToString())
        assertTrue(element.children.isEmpty())
    }

    // ── OID inside a SEQUENCE ───────────────────────────────────────

    @Test
    fun parseOidElement() {
        // OID 1.2.840.113549.1.1.11 (sha256WithRSAEncryption)
        // DER: 06 09 2A 86 48 86 F7 0D 01 01 0B
        val data = hex("06 09 2A 86 48 86 F7 0D 01 01 0B")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.OID, element.tag)
        val oid = Oid.fromDer(element.rawValue)
        assertEquals("1.2.840.113549.1.1.11", oid.dotNotation)
    }

    // ── Nested SEQUENCE ─────────────────────────────────────────────

    @Test
    fun parseNestedSequence() {
        // SEQUENCE { SEQUENCE { INTEGER 1 }, INTEGER 2 }
        // Inner: 30 03 02 01 01
        // Outer: 30 08 30 03 02 01 01 02 01 02
        val data = hex("30 08 30 03 02 01 01 02 01 02")
        val outer = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.SEQUENCE, outer.tag)
        assertEquals(2, outer.children.size)

        val inner = outer.childAt(0)!!
        assertEquals(Asn1Tag.SEQUENCE, inner.tag)
        assertEquals(1, inner.children.size)

        val innerInt = inner.childAt(0)!!
        assertEquals(Asn1Tag.INTEGER, innerInt.tag)
        assertEquals(0x01, innerInt.rawValue[0].toInt() and 0xFF)

        val outerInt = outer.childAt(1)!!
        assertEquals(Asn1Tag.INTEGER, outerInt.tag)
        assertEquals(0x02, outerInt.rawValue[0].toInt() and 0xFF)
    }

    // ── Context-specific tags ───────────────────────────────────────

    @Test
    fun parseContextSpecificPrimitive() {
        // [0] IMPLICIT primitive, 2 value bytes
        // a0 bit pattern for context-specific + primitive + tag 0 = 0x80
        val data = hex("80 02 AB CD")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.TagClass.CONTEXT_SPECIFIC, element.tag.tagClass)
        assertEquals(0, element.tag.tagNumber)
        assertEquals(false, element.tag.isConstructed)
        assertEquals(2, element.rawValue.size)
    }

    @Test
    fun parseContextSpecificConstructed() {
        // [0] EXPLICIT constructed wrapping an INTEGER
        // Tag byte: 0xA0 (context-specific + constructed + tag 0)
        val data = hex("A0 03 02 01 07")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.TagClass.CONTEXT_SPECIFIC, element.tag.tagClass)
        assertEquals(0, element.tag.tagNumber)
        assertTrue(element.isConstructed)
        assertEquals(1, element.children.size)
        assertEquals(Asn1Tag.INTEGER, element.children[0].tag)
    }

    // ── Multi-byte length ───────────────────────────────────────────

    @Test
    fun parseMultiByteLength() {
        // OCTET STRING with 200 bytes of zeros
        // Length 200 = 0xC8 → need long form: 81 C8
        val valueBytes = ByteArray(200)
        val header = hex("04 81 C8")
        val data = header + valueBytes
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.OCTET_STRING, element.tag)
        assertEquals(200, element.rawValue.size)
    }

    @Test
    fun parseTwoByteLength() {
        // OCTET STRING with 256 bytes → length 0x0100 → long form: 82 01 00
        val valueBytes = ByteArray(256)
        val header = hex("04 82 01 00")
        val data = header + valueBytes
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.OCTET_STRING, element.tag)
        assertEquals(256, element.rawValue.size)
    }

    // ── BIT STRING ──────────────────────────────────────────────────

    @Test
    fun parseBitString() {
        // BIT STRING with 0 unused bits and 2 content bytes
        val data = hex("03 03 00 FF AA")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.BIT_STRING, element.tag)
        assertEquals(3, element.rawValue.size)
        // First value byte = unused-bits count
        assertEquals(0x00, element.rawValue[0].toInt() and 0xFF)
    }

    // ── NULL ────────────────────────────────────────────────────────

    @Test
    fun parseNull() {
        val data = hex("05 00")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.NULL, element.tag)
        assertEquals(0, element.rawValue.size)
    }

    // ── SET ─────────────────────────────────────────────────────────

    @Test
    fun parseSet() {
        // SET { INTEGER 3, INTEGER 7 }
        val data = hex("31 06 02 01 03 02 01 07")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.SET, element.tag)
        assertTrue(element.isConstructed)
        assertEquals(2, element.children.size)
    }

    // ── parseAll ────────────────────────────────────────────────────

    @Test
    fun parseAllMultipleElements() {
        // Two consecutive INTEGERs
        val data = hex("02 01 01 02 01 02")
        val elements = Asn1Parser.parseAll(data)

        assertEquals(2, elements.size)
        assertEquals(Asn1Tag.INTEGER, elements[0].tag)
        assertEquals(Asn1Tag.INTEGER, elements[1].tag)
    }

    // ── findChild / findChildren ────────────────────────────────────

    @Test
    fun findChildAndFindChildren() {
        // SEQUENCE { INTEGER 1, OID 1.2, INTEGER 2 }
        val data = hex("30 09 02 01 01 06 01 2A 02 01 02")
        val seq = Asn1Parser.parse(data)

        assertNotNull(seq.findChild(Asn1Tag.OID))
        assertEquals(2, seq.findChildren(Asn1Tag.INTEGER).size)
        assertNull(seq.findChild(Asn1Tag.BOOLEAN))
    }

    // ── fullEncoding ────────────────────────────────────────────────

    @Test
    fun fullEncodingMatchesInput() {
        val data = hex("30 06 02 01 05 02 01 0A")
        val element = Asn1Parser.parse(data)
        assertTrue(data.contentEquals(element.fullEncoding))
    }

    // ── Multi-byte tag number ───────────────────────────────────────

    @Test
    fun parseMultiByteTagNumber() {
        // Application class, tag number 50 (> 31)
        // First byte: 0x40 | 0x1F = 0x5F (application, primitive, long form)
        // Tag number 50 = 0x32 → single continuation byte: 0x32
        // Then length 1, value 0xFF
        val data = hex("5F 32 01 FF")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.TagClass.APPLICATION, element.tag.tagClass)
        assertEquals(50, element.tag.tagNumber)
        assertEquals(false, element.tag.isConstructed)
    }

    // ── Empty sequence ──────────────────────────────────────────────

    @Test
    fun parseEmptySequence() {
        val data = hex("30 00")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.SEQUENCE, element.tag)
        assertTrue(element.children.isEmpty())
    }

    // ── Error cases ─────────────────────────────────────────────────

    @Test
    fun emptyInputThrows() {
        assertFailsWith<Asn1ParseException> {
            Asn1Parser.parse(ByteArray(0))
        }
    }

    @Test
    fun truncatedDataThrows() {
        // SEQUENCE says length 6 but we only provide 3 value bytes
        assertFailsWith<Asn1ParseException> {
            Asn1Parser.parse(hex("30 06 02 01 05"))
        }
    }

    @Test
    fun truncatedLengthThrows() {
        // Tag byte only, no length
        assertFailsWith<Asn1ParseException> {
            Asn1Parser.parse(hex("30"))
        }
    }

    @Test
    fun indefiniteLengthThrows() {
        // 0x80 as length byte = indefinite (not supported)
        assertFailsWith<Asn1ParseException> {
            Asn1Parser.parse(hex("30 80 00 00"))
        }
    }

    @Test
    fun truncatedMultiByteTagThrows() {
        // Long-form tag number starting but stream ends
        assertFailsWith<Asn1ParseException> {
            Asn1Parser.parse(hex("9F"))
        }
    }

    // ── String types ────────────────────────────────────────────────

    @Test
    fun parseUtf8String() {
        // UTF8String "abc"
        val data = hex("0C 03 61 62 63")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.UTF8_STRING, element.tag)
        assertEquals("abc", element.rawValue.decodeToString())
    }

    @Test
    fun parsePrintableString() {
        // PrintableString "AB"
        val data = hex("13 02 41 42")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.PRINTABLE_STRING, element.tag)
        assertEquals("AB", element.rawValue.decodeToString())
    }

    // ── BOOLEAN ─────────────────────────────────────────────────────

    @Test
    fun parseBoolean() {
        // BOOLEAN TRUE (0xFF)
        val data = hex("01 01 FF")
        val element = Asn1Parser.parse(data)

        assertEquals(Asn1Tag.BOOLEAN, element.tag)
        assertEquals(0xFF.toByte(), element.rawValue[0])
    }
}
