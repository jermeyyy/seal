package com.jermey.seal.core.model

/**
 * Version of the Signed Certificate Timestamp (SCT) structure as defined in RFC 6962.
 *
 * @property value The wire-format integer value of this version.
 * @see SignedCertificateTimestamp
 */
public enum class SctVersion(public val value: Int) {
    /** SCT version 1 as defined in RFC 6962. */
    V1(0);

    public companion object {
        /**
         * Look up an [SctVersion] by its wire-format [value].
         *
         * @param value The integer version value from the SCT binary encoding.
         * @return The corresponding [SctVersion], or `null` if the value is unknown.
         */
        public fun fromValue(value: Int): SctVersion? = entries.find { it.value == value }
    }
}
