package com.jermey.seal.core.parser

import com.jermey.seal.core.model.DigitallySigned
import com.jermey.seal.core.model.HashAlgorithm
import com.jermey.seal.core.model.LogId
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SctVersion
import com.jermey.seal.core.model.SignatureAlgorithm
import com.jermey.seal.core.model.SignedCertificateTimestamp
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SctDeserializerTest {

    // -- Helper: build raw SCT bytes manually --

    private fun buildSctBytes(
        version: Byte = 0x00,
        logId: ByteArray = ByteArray(32) { it.toByte() },
        timestampMs: Long = 1_672_531_200_000L, // 2023-01-01T00:00:00Z
        extensions: ByteArray = byteArrayOf(),
        hashAlgorithm: Byte = 0x04, // SHA256
        signatureAlgorithm: Byte = 0x03, // ECDSA
        signature: ByteArray = ByteArray(71) { (it + 0xAA).toByte() },
    ): ByteArray {
        val buffer = mutableListOf<Byte>()

        // Version (1 byte)
        buffer.add(version)

        // LogID (32 bytes)
        logId.forEach { buffer.add(it) }

        // Timestamp (8 bytes, big-endian)
        for (i in 56 downTo 0 step 8) {
            buffer.add(((timestampMs ushr i) and 0xFF).toByte())
        }

        // Extensions length (2 bytes, big-endian) + extensions
        buffer.add(((extensions.size shr 8) and 0xFF).toByte())
        buffer.add((extensions.size and 0xFF).toByte())
        extensions.forEach { buffer.add(it) }

        // DigitallySigned: hash_alg(1) + sig_alg(1) + sig_len(2) + sig
        buffer.add(hashAlgorithm)
        buffer.add(signatureAlgorithm)
        buffer.add(((signature.size shr 8) and 0xFF).toByte())
        buffer.add((signature.size and 0xFF).toByte())
        signature.forEach { buffer.add(it) }

        return buffer.toByteArray()
    }

    private fun buildSctListBytes(vararg scts: ByteArray): ByteArray {
        val inner = mutableListOf<Byte>()
        for (sct in scts) {
            // Per-SCT length prefix (2 bytes)
            inner.add(((sct.size shr 8) and 0xFF).toByte())
            inner.add((sct.size and 0xFF).toByte())
            sct.forEach { inner.add(it) }
        }
        val innerBytes = inner.toByteArray()

        // Total list length prefix (2 bytes) + inner bytes
        val buffer = mutableListOf<Byte>()
        buffer.add(((innerBytes.size shr 8) and 0xFF).toByte())
        buffer.add((innerBytes.size and 0xFF).toByte())
        innerBytes.forEach { buffer.add(it) }

        return buffer.toByteArray()
    }

    // -- SctDeserializer tests --

    @Test
    fun validSctIsParsedCorrectly() {
        val logIdBytes = ByteArray(32) { it.toByte() }
        val timestampMs = 1_672_531_200_000L
        val extensionData = byteArrayOf(0x01, 0x02, 0x03)
        val signatureData = ByteArray(71) { (it + 0xAA).toByte() }

        val bytes = buildSctBytes(
            version = 0x00,
            logId = logIdBytes,
            timestampMs = timestampMs,
            extensions = extensionData,
            hashAlgorithm = 0x04,
            signatureAlgorithm = 0x03,
            signature = signatureData,
        )

        val result = SctDeserializer.deserialize(bytes, Origin.EMBEDDED)
        assertTrue(result.isSuccess, "Expected success but got: ${result.exceptionOrNull()}")

        val sct = result.getOrThrow()
        assertEquals(SctVersion.V1, sct.version)
        assertEquals(LogId(logIdBytes), sct.logId)
        assertEquals(Instant.fromEpochMilliseconds(timestampMs), sct.timestamp)
        assertTrue(extensionData.contentEquals(sct.extensions))
        assertEquals(HashAlgorithm.SHA256, sct.signature.hashAlgorithm)
        assertEquals(SignatureAlgorithm.ECDSA, sct.signature.signatureAlgorithm)
        assertTrue(signatureData.contentEquals(sct.signature.signature))
        assertEquals(Origin.EMBEDDED, sct.origin)
    }

    @Test
    fun emptyExtensionsParsedCorrectly() {
        val bytes = buildSctBytes(extensions = byteArrayOf())
        val result = SctDeserializer.deserialize(bytes, Origin.TLS_EXTENSION)
        assertTrue(result.isSuccess, "Expected success but got: ${result.exceptionOrNull()}")

        val sct = result.getOrThrow()
        assertTrue(sct.extensions.isEmpty())
        assertEquals(Origin.TLS_EXTENSION, sct.origin)
    }

    @Test
    fun tooShortInputReturnsFailure() {
        val result = SctDeserializer.deserialize(byteArrayOf(0x00), Origin.EMBEDDED)
        assertTrue(result.isFailure)
    }

    @Test
    fun emptyInputReturnsFailure() {
        val result = SctDeserializer.deserialize(byteArrayOf(), Origin.EMBEDDED)
        assertTrue(result.isFailure)
    }

    @Test
    fun invalidVersionReturnsFailure() {
        val bytes = buildSctBytes(version = 0x05)
        val result = SctDeserializer.deserialize(bytes, Origin.EMBEDDED)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("version"))
    }

    @Test
    fun truncatedSignatureReturnsFailure() {
        val fullBytes = buildSctBytes()
        // Cut off the last 10 bytes to truncate the signature
        val truncated = fullBytes.copyOfRange(0, fullBytes.size - 10)
        val result = SctDeserializer.deserialize(truncated, Origin.EMBEDDED)
        assertTrue(result.isFailure)
    }

    @Test
    fun truncatedTimestampReturnsFailure() {
        // Only version + partial logId (too short for timestamp)
        val truncated = ByteArray(1 + 32 + 3) // version + logId + 3 bytes (short timestamp)
        truncated[0] = 0x00
        val result = SctDeserializer.deserialize(truncated, Origin.EMBEDDED)
        assertTrue(result.isFailure)
    }

    @Test
    fun invalidHashAlgorithmReturnsFailure() {
        val bytes = buildSctBytes(hashAlgorithm = 0xFF.toByte())
        val result = SctDeserializer.deserialize(bytes, Origin.EMBEDDED)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("hash algorithm"))
    }

    @Test
    fun invalidSignatureAlgorithmReturnsFailure() {
        val bytes = buildSctBytes(signatureAlgorithm = 0xFF.toByte())
        val result = SctDeserializer.deserialize(bytes, Origin.EMBEDDED)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("signature algorithm"))
    }

    // -- SctListParser tests --

    @Test
    fun validSctListWithSingleEntry() {
        val sctBytes = buildSctBytes()
        val listBytes = buildSctListBytes(sctBytes)

        val results = SctListParser.parse(listBytes, Origin.EMBEDDED)
        assertEquals(1, results.size)
        assertEquals(SctVersion.V1, results[0].version)
    }

    @Test
    fun validSctListWithMultipleEntries() {
        val sct1 = buildSctBytes(timestampMs = 1_000_000_000L)
        val sct2 = buildSctBytes(timestampMs = 2_000_000_000L)
        val listBytes = buildSctListBytes(sct1, sct2)

        val results = SctListParser.parse(listBytes, Origin.TLS_EXTENSION)
        assertEquals(2, results.size)
        assertEquals(Instant.fromEpochMilliseconds(1_000_000_000L), results[0].timestamp)
        assertEquals(Instant.fromEpochMilliseconds(2_000_000_000L), results[1].timestamp)
    }

    @Test
    fun sctListSkipsMalformedEntries() {
        val validSct = buildSctBytes()
        val malformedSct = byteArrayOf(0xFF.toByte(), 0x01, 0x02) // invalid version, too short
        val listBytes = buildSctListBytes(malformedSct, validSct)

        val results = SctListParser.parse(listBytes, Origin.EMBEDDED)
        // The malformed SCT should be skipped, valid one parsed
        assertEquals(1, results.size)
        assertEquals(SctVersion.V1, results[0].version)
    }

    @Test
    fun emptyListBytesReturnsEmptyList() {
        val results = SctListParser.parse(byteArrayOf(), Origin.EMBEDDED)
        assertTrue(results.isEmpty())
    }

    @Test
    fun zeroLengthListReturnsEmptyList() {
        val results = SctListParser.parse(byteArrayOf(0x00, 0x00), Origin.EMBEDDED)
        assertTrue(results.isEmpty())
    }

    @Test
    fun tooShortListBytesReturnsEmptyList() {
        val results = SctListParser.parse(byteArrayOf(0x01), Origin.EMBEDDED)
        assertTrue(results.isEmpty())
    }
}
