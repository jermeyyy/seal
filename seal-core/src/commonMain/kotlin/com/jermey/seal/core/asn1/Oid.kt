package com.jermey.seal.core.asn1

/**
 * Represents an ASN.1 Object Identifier (OID).
 *
 * An OID is a sequence of non-negative integer components that uniquely identifies
 * an object in the ASN.1 registration hierarchy.
 */
public class Oid(public val components: List<Int>) {

    /** The OID in dot-notation form, e.g. `"1.2.840.113549.1.1.11"`. */
    public val dotNotation: String get() = components.joinToString(".")

    override fun toString(): String = dotNotation

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Oid) return false
        return components == other.components
    }

    override fun hashCode(): Int = components.hashCode()

    public companion object {
        /**
         * Parse an OID from its dot-notation string, e.g. `"1.2.840.113549.1.1.11"`.
         *
         * @throws Asn1ParseException if the string is malformed.
         */
        public fun fromDotNotation(oid: String): Oid {
            if (oid.isBlank()) throw Asn1ParseException("OID string must not be blank")
            val parts = oid.split('.')
            if (parts.size < 2) throw Asn1ParseException("OID must have at least two components: $oid")
            val ints = parts.map { part ->
                part.toIntOrNull() ?: throw Asn1ParseException("Invalid OID component '$part' in: $oid")
            }
            return Oid(ints)
        }

        /**
         * Decode an OID from its DER-encoded value bytes (content octets only, not including
         * the tag and length octets).
         *
         * @throws Asn1ParseException if the bytes are malformed.
         */
        public fun fromDer(bytes: ByteArray): Oid {
            if (bytes.isEmpty()) throw Asn1ParseException("OID value bytes must not be empty")

            val components = mutableListOf<Int>()

            // First byte encodes the first two components: first * 40 + second
            val firstByte = bytes[0].toInt() and 0xFF
            val first = if (firstByte >= 80) 2 else firstByte / 40
            val second = if (firstByte >= 80) firstByte - 80 else firstByte % 40
            components.add(first)
            components.add(second)

            // Remaining bytes encode subsequent components in base-128 with high-bit continuation
            var i = 1
            while (i < bytes.size) {
                var value = 0
                var byte: Int
                do {
                    if (i >= bytes.size) {
                        throw Asn1ParseException("Truncated OID component at byte index $i")
                    }
                    byte = bytes[i].toInt() and 0xFF
                    value = (value shl 7) or (byte and 0x7F)
                    i++
                } while (byte and 0x80 != 0)
                components.add(value)
            }

            return Oid(components)
        }
    }
}
