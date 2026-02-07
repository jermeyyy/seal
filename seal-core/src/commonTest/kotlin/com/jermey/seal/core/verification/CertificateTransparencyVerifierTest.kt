package com.jermey.seal.core.verification

import com.jermey.seal.core.crypto.CryptoVerifier
import com.jermey.seal.core.loglist.LogListDataSource
import com.jermey.seal.core.loglist.LogListService
import com.jermey.seal.core.loglist.RawLogList
import com.jermey.seal.core.model.SignatureAlgorithm
import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.policy.CTPolicy
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalEncodingApi::class)
class CertificateTransparencyVerifierTest {

    // ---- Fakes ----

    private class FakeCryptoVerifier(
        private val verifyResult: Boolean = true,
    ) : CryptoVerifier {
        override fun verifySignature(
            publicKeyBytes: ByteArray,
            data: ByteArray,
            signature: ByteArray,
            algorithm: SignatureAlgorithm,
        ): Boolean = verifyResult

        override fun sha256(data: ByteArray): ByteArray = ByteArray(32)
    }

    private class FakeLogListDataSource(
        private val result: Result<RawLogList>,
    ) : LogListDataSource {
        override suspend fun fetchLogList(): Result<RawLogList> = result
    }

    // ---- Shared test fixtures ----

    /** Minimal valid DER certificate: SEQUENCE { SEQUENCE {} } – parseable but has no extensions/SCTs. */
    private val minimalCertDer = byteArrayOf(0x30, 0x02, 0x30, 0x00)

    /** Dummy issuer certificate (same minimal structure). */
    private val issuerCertDer = byteArrayOf(0x30, 0x02, 0x30, 0x00)

    /** 32-byte log ID shared between SCTs and the test log list. */
    private val testLogId = ByteArray(32) { it.toByte() }
    private val testLogIdBase64 = Base64.encode(testLogId)

    /** Arbitrary public key bytes (used in the log list JSON). */
    private val testKeyBytes = ByteArray(65) { (it + 10).toByte() }
    private val testKeyBase64 = Base64.encode(testKeyBytes)

    /** A different 32-byte log ID that does NOT appear in the log list. */
    private val unknownLogId = ByteArray(32) { (it + 0x80).toByte() }

    // ---- Helpers ----

    /**
     * Build a minimal V3 CT log list JSON with a single operator / single log entry.
     */
    private fun buildLogListJson(
        logId: String = testLogIdBase64,
        key: String = testKeyBase64,
        state: String = """{ "usable": { "timestamp": "2024-01-01T00:00:00Z" } }""",
    ): String = """
        {
            "operators": [{
                "name": "Test Operator",
                "logs": [{
                    "log_id": "$logId",
                    "key": "$key",
                    "url": "https://ct.test/",
                    "state": $state,
                    "temporal_interval": {
                        "start_inclusive": "2024-01-01T00:00:00Z",
                        "end_exclusive": "2026-01-01T00:00:00Z"
                    }
                }]
            }]
        }
    """.trimIndent()

    /**
     * Produce a single SCT in binary (RFC 6962) format.
     */
    private fun buildSctBytes(
        logId: ByteArray = testLogId,
        timestampMs: Long = 1_672_531_200_000L, // 2023-01-01T00:00:00Z
        hashAlgorithm: Byte = 0x04, // SHA-256
        signatureAlgorithm: Byte = 0x03, // ECDSA
        signature: ByteArray = ByteArray(71) { (it + 0xAA).toByte() },
    ): ByteArray {
        val buffer = mutableListOf<Byte>()
        // version (1 byte)
        buffer.add(0x00)
        // logId (32 bytes)
        logId.forEach { buffer.add(it) }
        // timestamp (8 bytes, big-endian)
        for (i in 56 downTo 0 step 8) {
            buffer.add(((timestampMs ushr i) and 0xFF).toByte())
        }
        // extensions length (2 bytes) + extensions (none)
        buffer.add(0x00)
        buffer.add(0x00)
        // DigitallySigned: hash_alg(1) + sig_alg(1) + sig_len(2) + sig
        buffer.add(hashAlgorithm)
        buffer.add(signatureAlgorithm)
        buffer.add(((signature.size shr 8) and 0xFF).toByte())
        buffer.add((signature.size and 0xFF).toByte())
        signature.forEach { buffer.add(it) }
        return buffer.toByteArray()
    }

    /**
     * Wrap one or more individual SCT byte arrays into the TLS SCT-list format:
     * 2-byte total length, then per-SCT 2-byte length + SCT bytes.
     */
    private fun buildSctListBytes(vararg scts: ByteArray): ByteArray {
        val inner = mutableListOf<Byte>()
        for (sct in scts) {
            inner.add(((sct.size shr 8) and 0xFF).toByte())
            inner.add((sct.size and 0xFF).toByte())
            sct.forEach { inner.add(it) }
        }
        val innerBytes = inner.toByteArray()
        val buffer = mutableListOf<Byte>()
        buffer.add(((innerBytes.size shr 8) and 0xFF).toByte())
        buffer.add((innerBytes.size and 0xFF).toByte())
        innerBytes.forEach { buffer.add(it) }
        return buffer.toByteArray()
    }

    /**
     * Create a [LogListService] backed by a fake embedded source returning the given JSON.
     */
    private fun makeLogListService(json: String = buildLogListJson()): LogListService {
        val source = FakeLogListDataSource(
            Result.success(RawLogList(json.encodeToByteArray(), null)),
        )
        return LogListService(networkSource = null, embeddedSource = source)
    }

    /**
     * A simple "pass-through" policy: returns [VerificationResult.Success.Trusted] when there is at
     * least one [SctVerificationResult.Valid], otherwise returns [VerificationResult.Failure.LogServersFailed].
     */
    private val passThroughPolicy = CTPolicy { _, sctResults ->
        val valid = sctResults.filterIsInstance<SctVerificationResult.Valid>()
        if (valid.isNotEmpty()) {
            VerificationResult.Success.Trusted(valid)
        } else {
            VerificationResult.Failure.LogServersFailed(sctResults)
        }
    }

    /**
     * Convenience factory for the verifier under test.
     */
    private fun makeVerifier(
        cryptoVerifier: CryptoVerifier = FakeCryptoVerifier(),
        logListService: LogListService = makeLogListService(),
        policy: CTPolicy = passThroughPolicy,
    ): CertificateTransparencyVerifier =
        CertificateTransparencyVerifier(cryptoVerifier, logListService, policy)

    // ---- Tests ----

    @Test
    fun emptyCertificateChainReturnsNoScts() = runTest {
        val result = makeVerifier().verify(emptyList())
        assertIs<VerificationResult.Failure.NoScts>(result)
    }

    @Test
    fun certificateWithNoSctsReturnsNoScts() = runTest {
        // Minimal cert has no SCT extension; no TLS/OCSP bytes → no SCTs at all.
        val result = makeVerifier().verify(listOf(minimalCertDer))
        assertIs<VerificationResult.Failure.NoScts>(result)
    }

    @Test
    fun logListFailureReturnsUnknownError() = runTest {
        val failingSource = FakeLogListDataSource(
            Result.failure(RuntimeException("network down")),
        )
        val logListService = LogListService(
            networkSource = null,
            embeddedSource = failingSource,
        )
        val verifier = makeVerifier(logListService = logListService)
        val sctList = buildSctListBytes(buildSctBytes())

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        assertIs<VerificationResult.Failure.UnknownError>(result)
    }

    @Test
    fun sctFromUnknownLogReturnsLogNotTrusted() = runTest {
        // SCT references a logId that is NOT in the log list → LogNotTrusted
        val sctList = buildSctListBytes(buildSctBytes(logId = unknownLogId))
        val verifier = makeVerifier()

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        // The pass-through policy converts 0 valid SCTs → LogServersFailed
        assertIs<VerificationResult.Failure.LogServersFailed>(result)
        assertTrue(result.sctResults.all { it is SctVerificationResult.Invalid.LogNotTrusted })
    }

    @Test
    fun sctFromRejectedLogReturnsLogRejected() = runTest {
        // Log list entry has REJECTED state
        val json = buildLogListJson(
            state = """{ "rejected": { "timestamp": "2024-06-01T00:00:00Z" } }""",
        )
        val verifier = makeVerifier(logListService = makeLogListService(json))
        val sctList = buildSctListBytes(buildSctBytes())

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        assertIs<VerificationResult.Failure.LogServersFailed>(result)
        assertTrue(result.sctResults.all { it is SctVerificationResult.Invalid.LogRejected })
    }

    @Test
    fun validSctWithPassingPolicyReturnsTrusted() = runTest {
        val sctList = buildSctListBytes(buildSctBytes())
        val verifier = makeVerifier(
            cryptoVerifier = FakeCryptoVerifier(verifyResult = true),
        )

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        assertIs<VerificationResult.Success.Trusted>(result)
        assertTrue(result.validScts.isNotEmpty())
    }

    @Test
    fun policyFailureIsPropagated() = runTest {
        val sctList = buildSctListBytes(buildSctBytes())

        // Policy always returns TooFewSctsTrusted regardless of input
        val strictPolicy = CTPolicy { _, _ ->
            VerificationResult.Failure.TooFewSctsTrusted(found = 1, required = 3)
        }
        val verifier = makeVerifier(
            cryptoVerifier = FakeCryptoVerifier(verifyResult = true),
            policy = strictPolicy,
        )

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertTrue(result.found == 1 && result.required == 3)
    }

    @Test
    fun tlsExtensionSctsAreIncluded() = runTest {
        // Verify that SCTs supplied via the tlsExtensionSctBytes parameter are picked up
        val sctList = buildSctListBytes(buildSctBytes())
        val verifier = makeVerifier(
            cryptoVerifier = FakeCryptoVerifier(verifyResult = true),
        )

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = sctList,
        )

        assertIs<VerificationResult.Success.Trusted>(result)
        assertTrue(result.validScts.size == 1)
    }

    @Test
    fun multipleSctSourcesCombined() = runTest {
        // Provide one SCT via TLS extension and one via OCSP – both should reach the policy
        val tlsSctList = buildSctListBytes(buildSctBytes())
        val ocspSctList = buildSctListBytes(buildSctBytes())

        var evaluatedCount = 0
        val countingPolicy = CTPolicy { _, sctResults ->
            evaluatedCount = sctResults.size
            val valid = sctResults.filterIsInstance<SctVerificationResult.Valid>()
            if (valid.isNotEmpty()) {
                VerificationResult.Success.Trusted(valid)
            } else {
                VerificationResult.Failure.LogServersFailed(sctResults)
            }
        }

        val verifier = makeVerifier(
            cryptoVerifier = FakeCryptoVerifier(verifyResult = true),
            policy = countingPolicy,
        )

        val result = verifier.verify(
            listOf(minimalCertDer, issuerCertDer),
            tlsExtensionSctBytes = tlsSctList,
            ocspResponseSctBytes = ocspSctList,
        )

        assertIs<VerificationResult.Success.Trusted>(result)
        // Both the TLS and OCSP SCTs should have been forwarded to the policy
        assertTrue(evaluatedCount == 2, "Expected 2 SCTs evaluated but got $evaluatedCount")
    }
}
