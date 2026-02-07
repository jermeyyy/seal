package com.jermey.seal.core.asn1

/**
 * Represents a parsed ASN.1 DER element (TLV — Tag, Length, Value).
 *
 * For constructed types (SEQUENCE, SET, context-specific constructed), the [children]
 * list contains the recursively parsed sub-elements. For primitive types, [children] is empty.
 */
public class Asn1Element(
    /** The ASN.1 tag of this element. */
    public val tag: Asn1Tag,
    /** The raw value bytes (content octets, excluding tag and length bytes). */
    public val rawValue: ByteArray,
    /** Recursively parsed children; non-empty only for constructed types. */
    public val children: List<Asn1Element>,
    /** The complete TLV encoding (tag + length + value bytes). */
    public val fullEncoding: ByteArray,
) {
    /** Whether this element is a constructed type. */
    public val isConstructed: Boolean get() = tag.isConstructed

    /** Find the first child whose tag equals [childTag], or `null`. */
    public fun findChild(childTag: Asn1Tag): Asn1Element? =
        children.firstOrNull { it.tag == childTag }

    /** Find all children whose tag equals [childTag]. */
    public fun findChildren(childTag: Asn1Tag): List<Asn1Element> =
        children.filter { it.tag == childTag }

    /** Return the child at [index], or `null` if out of bounds. */
    public fun childAt(index: Int): Asn1Element? =
        children.getOrNull(index)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Asn1Element) return false
        return tag == other.tag &&
            rawValue.contentEquals(other.rawValue) &&
            children == other.children
    }

    override fun hashCode(): Int {
        var result = tag.hashCode()
        result = 31 * result + rawValue.contentHashCode()
        result = 31 * result + children.hashCode()
        return result
    }

    override fun toString(): String = buildString {
        append("Asn1Element(tag=$tag, valueLen=${rawValue.size}")
        if (children.isNotEmpty()) {
            append(", children=${children.size}")
        }
        append(")")
    }
}
