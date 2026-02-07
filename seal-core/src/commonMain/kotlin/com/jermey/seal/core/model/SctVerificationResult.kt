package com.jermey.seal.core.model

/**
 * Result of verifying a single [SignedCertificateTimestamp] against a log server's public key.
 *
 * Each SCT is independently verified and produces either a [Valid] or [Invalid] result,
 * which is then aggregated by the [CTPolicy][com.jermey.seal.core.policy.CTPolicy] to
 * determine overall CT compliance.
 *
 * @see VerificationResult
 */
public sealed class SctVerificationResult {

    /**
     * The SCT's signature was successfully verified against the log's public key.
     *
     * @property sct The verified SCT.
     * @property logOperator The name of the operator of the log that issued this SCT, or `null` if unknown.
     */
    public data class Valid(
        public val sct: SignedCertificateTimestamp,
        public val logOperator: String?,
    ) : SctVerificationResult()

    /**
     * The SCT failed verification for one of several reasons.
     *
     * @property sct The SCT that failed verification.
     */
    public sealed class Invalid : SctVerificationResult() {
        public abstract val sct: SignedCertificateTimestamp

        /** Signature verification threw an exception (e.g., malformed key or data). */
        public data class FailedVerification(override val sct: SignedCertificateTimestamp) : Invalid()

        /** The SCT references a log ID not present in the trusted log list. */
        public data class LogNotTrusted(override val sct: SignedCertificateTimestamp) : Invalid()

        /** The SCT references a log whose temporal interval has expired. */
        public data class LogExpired(override val sct: SignedCertificateTimestamp) : Invalid()

        /** The SCT references a log that has been rejected (state = [LogState.REJECTED]). */
        public data class LogRejected(override val sct: SignedCertificateTimestamp) : Invalid()

        /** The SCT signature did not match the expected signed data. */
        public data class SignatureMismatch(override val sct: SignedCertificateTimestamp) : Invalid()
    }
}
