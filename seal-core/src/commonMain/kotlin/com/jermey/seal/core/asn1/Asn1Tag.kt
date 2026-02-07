package com.jermey.seal.core.asn1

/**
 * Represents an ASN.1 tag with class, constructed/primitive flag, and tag number.
 */
public class Asn1Tag(
    public val tagClass: TagClass,
    public val isConstructed: Boolean,
    public val tagNumber: Int,
) {
    /**
     * ASN.1 tag class encoded in the two high bits of the first tag byte.
     */
    public enum class TagClass(public val mask: Int) {
        UNIVERSAL(0x00),
        APPLICATION(0x40),
        CONTEXT_SPECIFIC(0x80),
        PRIVATE(0xC0),
        ;

        public companion object {
            /** Resolve a [TagClass] from the two high bits of a tag byte. */
            public fun fromByte(byte: Int): TagClass = when (byte and 0xC0) {
                0x00 -> UNIVERSAL
                0x40 -> APPLICATION
                0x80 -> CONTEXT_SPECIFIC
                0xC0 -> PRIVATE
                else -> throw Asn1ParseException("Invalid tag class bits: 0x${byte.toString(16)}")
            }
        }
    }

    /** Returns a copy of this tag with the [isConstructed] flag set to the given value. */
    public fun withConstructed(constructed: Boolean): Asn1Tag =
        Asn1Tag(tagClass, constructed, tagNumber)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asn1Tag) return false
        return tagClass == other.tagClass &&
            isConstructed == other.isConstructed &&
            tagNumber == other.tagNumber
    }

    override fun hashCode(): Int {
        var result = tagClass.hashCode()
        result = 31 * result + isConstructed.hashCode()
        result = 31 * result + tagNumber
        return result
    }

    override fun toString(): String {
        val classStr = tagClass.name
        val pc = if (isConstructed) "CONSTRUCTED" else "PRIMITIVE"
        return "Asn1Tag($classStr, $pc, 0x${tagNumber.toString(16).uppercase()})"
    }

    public companion object {
        // Well-known universal tags
        public val BOOLEAN: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x01)
        public val INTEGER: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x02)
        public val BIT_STRING: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x03)
        public val OCTET_STRING: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x04)
        public val NULL: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x05)
        public val OID: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x06)
        public val UTF8_STRING: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x0C)
        public val PRINTABLE_STRING: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x13)
        public val IA5_STRING: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x16)
        public val UTC_TIME: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x17)
        public val GENERALIZED_TIME: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, false, 0x18)
        public val SEQUENCE: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, true, 0x10)
        public val SET: Asn1Tag = Asn1Tag(TagClass.UNIVERSAL, true, 0x11)

        /** Create a context-specific tag ([0], [1], etc.). */
        public fun contextSpecific(tagNumber: Int, constructed: Boolean = false): Asn1Tag =
            Asn1Tag(TagClass.CONTEXT_SPECIFIC, constructed, tagNumber)
    }
}
