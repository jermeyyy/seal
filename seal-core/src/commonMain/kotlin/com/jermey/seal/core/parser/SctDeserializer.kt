package com.jermey.seal.core.parser

import com.jermey.seal.core.model.DigitallySigned
import com.jermey.seal.core.model.HashAlgorithm
import com.jermey.seal.core.model.LogId
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SctVersion
import com.jermey.seal.core.model.SignatureAlgorithm
import com.jermey.seal.core.model.SignedCertificateTimestamp
import kotlinx.datetime.Instant

/**
 * Parses a single SCT from raw bytes per RFC 6962 §3.3.
 *
 * Binary layout:
 * ```
 * struct {
 *     Version sct_version;          // 1 byte
 *     LogID id;                     // 32 bytes
 *     uint64 timestamp;             // 8 bytes (ms since epoch)
 *     opaque extensions<0..2^16-1>; // 2-byte length prefix + data
 *     DigitallySigned signature;    // hash_alg(1) + sig_alg(1) + sig_len(2) + sig
 * } SignedCertificateTimestamp;
 * ```
 */
public object SctDeserializer {

    private const val LOG_ID_LENGTH = 32
    private const val TIMESTAMP_LENGTH = 8
    private const val MIN_SCT_LENGTH = 1 + LOG_ID_LENGTH + TIMESTAMP_LENGTH + 2 + 4 // version + logId + timestamp + ext_len + sig header

    /**
     * Parses a single [SignedCertificateTimestamp] from the given [bytes].
     *
     * @param bytes Raw SCT bytes in RFC 6962 binary format.
     * @param origin The origin of the SCT (not encoded in the binary format).
     * @return [Result] containing the parsed SCT, or a failure with a descriptive error.
     */
    public fun deserialize(bytes: ByteArray, origin: Origin): Result<SignedCertificateTimestamp> = runCatching {
        var offset = 0

        fun remaining(): Int = bytes.size - offset

        fun requireBytes(count: Int, field: String) {
            if (remaining() < count) {
                error("Truncated SCT: need $count bytes for $field at offset $offset, but only ${remaining()} remain")
            }
        }

        fun readByte(field: String): Int {
            requireBytes(1, field)
            val value = bytes[offset].toInt() and 0xFF
            offset++
            return value
        }

        fun readUint16(field: String): Int {
            requireBytes(2, field)
            val value = ((bytes[offset].toInt() and 0xFF) shl 8) or
                (bytes[offset + 1].toInt() and 0xFF)
            offset += 2
            return value
        }

        fun readUint64(field: String): Long {
            requireBytes(TIMESTAMP_LENGTH, field)
            var value = 0L
            for (i in 0 until TIMESTAMP_LENGTH) {
                value = (value shl 8) or (bytes[offset + i].toLong() and 0xFF)
            }
            offset += TIMESTAMP_LENGTH
            return value
        }

        fun readBytes(count: Int, field: String): ByteArray {
            requireBytes(count, field)
            val result = bytes.copyOfRange(offset, offset + count)
            offset += count
            return result
        }

        // 1. Version (1 byte)
        val versionByte = readByte("sct_version")
        val version = SctVersion.fromValue(versionByte)
            ?: error("Unknown SCT version: $versionByte")

        // 2. LogID (32 bytes)
        val logIdBytes = readBytes(LOG_ID_LENGTH, "log_id")
        val logId = LogId(logIdBytes)

        // 3. Timestamp (8 bytes, milliseconds since epoch)
        val timestampMs = readUint64("timestamp")
        val timestamp = Instant.fromEpochMilliseconds(timestampMs)

        // 4. Extensions (2-byte length prefix + data)
        val extensionsLength = readUint16("extensions_length")
        val extensions = readBytes(extensionsLength, "extensions")

        // 5. DigitallySigned: hash_alg(1) + sig_alg(1) + sig_len(2) + signature
        val hashAlgByte = readByte("hash_algorithm")
        val hashAlgorithm = HashAlgorithm.fromValue(hashAlgByte)
            ?: error("Unknown hash algorithm: $hashAlgByte")

        val sigAlgByte = readByte("signature_algorithm")
        val signatureAlgorithm = SignatureAlgorithm.fromValue(sigAlgByte)
            ?: error("Unknown signature algorithm: $sigAlgByte")

        val signatureLength = readUint16("signature_length")
        val signatureBytes = readBytes(signatureLength, "signature")

        val digitallySigned = DigitallySigned(
            hashAlgorithm = hashAlgorithm,
            signatureAlgorithm = signatureAlgorithm,
            signature = signatureBytes,
        )

        SignedCertificateTimestamp(
            version = version,
            logId = logId,
            timestamp = timestamp,
            extensions = extensions,
            signature = digitallySigned,
            origin = origin,
        )
    }
}
