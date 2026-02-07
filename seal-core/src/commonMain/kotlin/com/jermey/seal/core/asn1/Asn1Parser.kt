package com.jermey.seal.core.asn1

/**
 * Read-only DER parser for ASN.1 structures.
 *
 * This parser supports:
 * - All four tag classes (UNIVERSAL, APPLICATION, CONTEXT_SPECIFIC, PRIVATE)
 * - Constructed and primitive types
 * - Multi-byte tag numbers (tag number ≥ 31)
 * - Multi-byte definite lengths (up to 4 length-of-length bytes)
 *
 * It does **not** support:
 * - BER indefinite-length encoding
 * - SET OF canonical ordering
 */
public object Asn1Parser {

    /**
     * Parse a single ASN.1 DER element from the beginning of [data].
     *
     * @throws Asn1ParseException on malformed input.
     */
    public fun parse(data: ByteArray): Asn1Element {
        if (data.isEmpty()) throw Asn1ParseException("Empty data")
        val (element, consumed) = parseElement(data, 0)
        return element
    }

    /**
     * Parse **all** consecutive ASN.1 DER elements in [data].
     *
     * @throws Asn1ParseException on malformed input.
     */
    public fun parseAll(data: ByteArray): List<Asn1Element> {
        val elements = mutableListOf<Asn1Element>()
        var offset = 0
        while (offset < data.size) {
            val (element, consumed) = parseElement(data, offset)
            elements.add(element)
            offset += consumed
        }
        return elements
    }

    // ── Internal parsing ──────────────────────────────────────────────

    /**
     * Parse one element starting at [offset] inside [data].
     *
     * @return pair of the parsed element and the total number of bytes consumed.
     */
    private fun parseElement(data: ByteArray, offset: Int): Pair<Asn1Element, Int> {
        val start = offset
        if (offset >= data.size) {
            throw Asn1ParseException("Unexpected end of data at offset $offset")
        }

        // ── Tag ──
        val (tag, tagLen) = readTag(data, offset)
        var pos = offset + tagLen

        // ── Length ──
        if (pos >= data.size) {
            throw Asn1ParseException("Truncated length at offset $pos")
        }
        val (length, lengthLen) = readLength(data, pos)
        pos += lengthLen

        // ── Value ──
        if (pos + length > data.size) {
            throw Asn1ParseException(
                "Value overflow: need $length bytes at offset $pos, but only ${data.size - pos} available"
            )
        }
        val rawValue = data.copyOfRange(pos, pos + length)
        val fullEncoding = data.copyOfRange(start, pos + length)

        // ── Children (constructed types) ──
        val children: List<Asn1Element> = if (tag.isConstructed) {
            parseChildren(rawValue)
        } else {
            emptyList()
        }

        val element = Asn1Element(tag, rawValue, children, fullEncoding)
        return element to (pos + length - start)
    }

    /**
     * Read tag byte(s) starting at [offset].
     *
     * @return pair of the [Asn1Tag] and the number of bytes consumed.
     */
    private fun readTag(data: ByteArray, offset: Int): Pair<Asn1Tag, Int> {
        val firstByte = data[offset].toInt() and 0xFF
        val tagClass = Asn1Tag.TagClass.fromByte(firstByte)
        val isConstructed = (firstByte and 0x20) != 0

        val lowBits = firstByte and 0x1F
        return if (lowBits < 0x1F) {
            // Short-form tag number
            Asn1Tag(tagClass, isConstructed, lowBits) to 1
        } else {
            // Long-form tag number (multi-byte)
            var tagNumber = 0
            var i = offset + 1
            while (true) {
                if (i >= data.size) {
                    throw Asn1ParseException("Truncated multi-byte tag at offset $i")
                }
                val b = data[i].toInt() and 0xFF
                tagNumber = (tagNumber shl 7) or (b and 0x7F)
                i++
                if (b and 0x80 == 0) break
            }
            Asn1Tag(tagClass, isConstructed, tagNumber) to (i - offset)
        }
    }

    /**
     * Read DER definite-length encoding starting at [offset].
     *
     * @return pair of the content length and the number of length bytes consumed.
     * @throws Asn1ParseException for indefinite length or unsupported long forms.
     */
    private fun readLength(data: ByteArray, offset: Int): Pair<Int, Int> {
        val firstByte = data[offset].toInt() and 0xFF
        return when {
            firstByte < 0x80 -> {
                // Short form
                firstByte to 1
            }

            firstByte == 0x80 -> {
                throw Asn1ParseException("Indefinite length encoding is not supported (DER only)")
            }

            else -> {
                // Long form: lower 7 bits = number of subsequent length bytes
                val numBytes = firstByte and 0x7F
                if (numBytes > 4) {
                    throw Asn1ParseException("Length encoding uses $numBytes bytes; max 4 supported")
                }
                if (offset + 1 + numBytes > data.size) {
                    throw Asn1ParseException(
                        "Truncated long-form length at offset $offset (need $numBytes more bytes)"
                    )
                }
                var length = 0
                for (i in 1..numBytes) {
                    length = (length shl 8) or (data[offset + i].toInt() and 0xFF)
                }
                if (length < 0) {
                    throw Asn1ParseException("Negative length: $length (overflow)")
                }
                length to (1 + numBytes)
            }
        }
    }

    /**
     * Recursively parse children from the value bytes of a constructed element.
     */
    private fun parseChildren(value: ByteArray): List<Asn1Element> {
        val children = mutableListOf<Asn1Element>()
        var offset = 0
        while (offset < value.size) {
            val (child, consumed) = parseElement(value, offset)
            children.add(child)
            offset += consumed
        }
        return children
    }
}
