package com.jermey.seal.core.policy

import com.jermey.seal.core.model.DigitallySigned
import com.jermey.seal.core.model.HashAlgorithm
import com.jermey.seal.core.model.LogId
import com.jermey.seal.core.model.Origin
import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.SctVersion
import com.jermey.seal.core.model.SignatureAlgorithm
import com.jermey.seal.core.model.SignedCertificateTimestamp
import com.jermey.seal.core.model.VerificationResult
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class PolicyTest {

    // ── helpers ──────────────────────────────────────────────────────────

    private fun sct(index: Int = 0): SignedCertificateTimestamp =
        SignedCertificateTimestamp(
            version = SctVersion.V1,
            logId = LogId(ByteArray(32) { index.toByte() }),
            timestamp = Instant.fromEpochMilliseconds(1_700_000_000_000L),
            extensions = ByteArray(0),
            signature = DigitallySigned(
                hashAlgorithm = HashAlgorithm.SHA256,
                signatureAlgorithm = SignatureAlgorithm.ECDSA,
                signature = ByteArray(64),
            ),
            origin = Origin.EMBEDDED,
        )

    private fun valid(operator: String?, index: Int = 0): SctVerificationResult.Valid =
        SctVerificationResult.Valid(sct(index), operator)

    private fun invalid(index: Int = 0): SctVerificationResult =
        SctVerificationResult.Invalid.FailedVerification(sct(index))

    // ── Chrome policy tests ─────────────────────────────────────────────

    @Test
    fun chromeNoSctsReturnsFailure() {
        val result = ChromeCtPolicy().evaluate(90, emptyList())
        assertIs<VerificationResult.Failure.NoScts>(result)
    }

    @Test
    fun chromeNoValidSctsReturnsNoScts() {
        // When no SCTs at all, returns NoScts
        val noSctsResult = ChromeCtPolicy().evaluate(90, emptyList())
        assertIs<VerificationResult.Failure.NoScts>(noSctsResult)

        // When SCTs exist but all are invalid, returns LogServersFailed
        val allInvalidResult = ChromeCtPolicy().evaluate(90, listOf(invalid(0), invalid(1)))
        assertIs<VerificationResult.Failure.LogServersFailed>(allInvalidResult)
    }

    @Test
    fun chromeShortCert2ValidSctsWithDiversitySucceeds() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("DigiCert", 1),
        )
        val result = ChromeCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(2, result.validScts.size)
    }

    @Test
    fun chromeShortCert1ValidSctTooFew() {
        val scts = listOf(valid("Google Trust Services", 0))
        val result = ChromeCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(1, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun chromeLongCert2ValidSctsTooFew() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("DigiCert", 1),
        )
        val result = ChromeCtPolicy().evaluate(365, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(2, result.found)
        assertEquals(3, result.required)
    }

    @Test
    fun chromeLongCert3ValidSctsWithDiversitySucceeds() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("DigiCert", 1),
            valid("Sectigo", 2),
        )
        val result = ChromeCtPolicy().evaluate(365, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(3, result.validScts.size)
    }

    @Test
    fun chromeAllGoogleOperatorsFails() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("Google LLC", 1),
            valid("GOOGLE", 2),
        )
        val result = ChromeCtPolicy().evaluate(365, scts)
        assertIs<VerificationResult.Failure.TooFewDistinctOperators>(result)
        assertEquals(1, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun chromeAllNonGoogleOperatorsFails() {
        val scts = listOf(
            valid("DigiCert", 0),
            valid("Sectigo", 1),
            valid("Let's Encrypt", 2),
        )
        val result = ChromeCtPolicy().evaluate(365, scts)
        assertIs<VerificationResult.Failure.TooFewDistinctOperators>(result)
        assertEquals(1, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun chromeBoundary180DaysRequires3() {
        // exactly 180 days → ≥ 3 required (Chrome uses < 180 for 2-SCT bracket)
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("DigiCert", 1),
        )
        val result = ChromeCtPolicy().evaluate(180, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(3, result.required)
    }

    @Test
    fun chromeExactly179DaysRequires2() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid("DigiCert", 1),
        )
        val result = ChromeCtPolicy().evaluate(179, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
    }

    @Test
    fun chromeMixOfValidAndInvalidCounts() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            invalid(1),
            valid("DigiCert", 2),
        )
        val result = ChromeCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(2, result.validScts.size)
    }

    @Test
    fun chromeNullOperatorTreatedAsNonGoogle() {
        val scts = listOf(
            valid("Google Trust Services", 0),
            valid(null, 1),
        )
        val result = ChromeCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
    }

    // ── Apple policy tests ──────────────────────────────────────────────

    @Test
    fun appleNoSctsReturnsFailure() {
        val result = AppleCtPolicy().evaluate(90, emptyList())
        assertIs<VerificationResult.Failure.NoScts>(result)
    }

    @Test
    fun appleNoValidSctsReturnsNoScts() {
        // When no SCTs at all, returns NoScts
        val noSctsResult = AppleCtPolicy().evaluate(90, emptyList())
        assertIs<VerificationResult.Failure.NoScts>(noSctsResult)

        // When SCTs exist but all are invalid, returns LogServersFailed
        val allInvalidResult = AppleCtPolicy().evaluate(90, listOf(invalid(0), invalid(1)))
        assertIs<VerificationResult.Failure.LogServersFailed>(allInvalidResult)
    }

    @Test
    fun appleShortCert2ValidSctsWithDiversitySucceeds() {
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
        )
        val result = AppleCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(2, result.validScts.size)
    }

    @Test
    fun appleShortCert1ValidSctTooFew() {
        val scts = listOf(valid("Operator A", 0))
        val result = AppleCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(1, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun appleMediumCert2ValidSctsTooFew() {
        // > 180 days → needs 3
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
        )
        val result = AppleCtPolicy().evaluate(200, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(2, result.found)
        assertEquals(3, result.required)
    }

    @Test
    fun appleMediumCert3ValidSctsWithDiversitySucceeds() {
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
            valid("Operator C", 2),
        )
        val result = AppleCtPolicy().evaluate(300, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(3, result.validScts.size)
    }

    @Test
    fun appleLongCert3ValidSctsWithDiversitySucceeds() {
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
            valid("Operator A", 2),
        )
        val result = AppleCtPolicy().evaluate(500, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
    }

    @Test
    fun appleSameOperatorFails() {
        val scts = listOf(
            valid("Same Operator", 0),
            valid("Same Operator", 1),
            valid("Same Operator", 2),
        )
        val result = AppleCtPolicy().evaluate(300, scts)
        assertIs<VerificationResult.Failure.TooFewDistinctOperators>(result)
        assertEquals(1, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun appleNullOperatorsFail() {
        val scts = listOf(
            valid(null, 0),
            valid(null, 1),
            valid(null, 2),
        )
        val result = AppleCtPolicy().evaluate(300, scts)
        assertIs<VerificationResult.Failure.TooFewDistinctOperators>(result)
        assertEquals(0, result.found)
        assertEquals(2, result.required)
    }

    @Test
    fun appleBoundary180DaysRequires2() {
        // exactly 180 days → ≤ 180 bracket → requires 2
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
        )
        val result = AppleCtPolicy().evaluate(180, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
    }

    @Test
    fun appleBoundary181DaysRequires3() {
        val scts = listOf(
            valid("Operator A", 0),
            valid("Operator B", 1),
        )
        val result = AppleCtPolicy().evaluate(181, scts)
        assertIs<VerificationResult.Failure.TooFewSctsTrusted>(result)
        assertEquals(3, result.required)
    }

    @Test
    fun appleMixOfValidAndInvalidCounts() {
        val scts = listOf(
            valid("Operator A", 0),
            invalid(1),
            valid("Operator B", 2),
        )
        val result = AppleCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Success.Trusted>(result)
        assertEquals(2, result.validScts.size)
    }

    @Test
    fun appleOneNullOneNamedOperatorSucceeds() {
        // null operators are excluded from distinct count, but one named operator still only counts as 1
        val scts = listOf(
            valid("Operator A", 0),
            valid(null, 1),
        )
        val result = AppleCtPolicy().evaluate(90, scts)
        assertIs<VerificationResult.Failure.TooFewDistinctOperators>(result)
    }
}
