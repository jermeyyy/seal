package com.jermey.seal.core.parser

import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SignedCertificateTimestamp

/**
 * Parses an SCT list as found in X.509 extensions and TLS extensions per RFC 6962.
 *
 * Binary layout:
 * ```
 * opaque SerializedSCT<1..2^16-1>;  // 2-byte length prefix per SCT
 * struct {
 *     SerializedSCT sct_list<1..2^16-1>;  // 2-byte total length prefix
 * } SignedCertificateTimestampList;
 * ```
 */
public object SctListParser {

    /**
     * Parses a list of [SignedCertificateTimestamp]s from the given [bytes].
     *
     * Malformed individual SCTs are skipped (the entire list is not rejected).
     *
     * @param bytes Raw SCT list bytes with a 2-byte total length prefix.
     * @param origin The origin of the SCTs.
     * @return A list of successfully parsed SCTs.
     */
    public fun parse(bytes: ByteArray, origin: Origin): List<SignedCertificateTimestamp> {
        if (bytes.size < 2) return emptyList()

        val totalLength = readUint16(bytes, 0)
        var offset = 2

        if (totalLength == 0) return emptyList()
        if (offset + totalLength > bytes.size) return emptyList()

        val listEnd = offset + totalLength
        val results = mutableListOf<SignedCertificateTimestamp>()

        while (offset + 2 <= listEnd) {
            val sctLength = readUint16(bytes, offset)
            offset += 2

            if (sctLength == 0 || offset + sctLength > listEnd) break

            val sctBytes = bytes.copyOfRange(offset, offset + sctLength)
            offset += sctLength

            SctDeserializer.deserialize(sctBytes, origin)
                .onSuccess { results.add(it) }
            // Malformed SCTs are silently skipped
        }

        return results
    }

    private fun readUint16(bytes: ByteArray, offset: Int): Int {
        return ((bytes[offset].toInt() and 0xFF) shl 8) or
            (bytes[offset + 1].toInt() and 0xFF)
    }
}
