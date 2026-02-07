package com.jermey.seal.core.model

public sealed class SctVerificationResult {
    public data class Valid(
        public val sct: SignedCertificateTimestamp,
        public val logOperator: String?,
    ) : SctVerificationResult()

    public sealed class Invalid : SctVerificationResult() {
        public abstract val sct: SignedCertificateTimestamp

        public data class FailedVerification(override val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogNotTrusted(override val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogExpired(override val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogRejected(override val sct: SignedCertificateTimestamp) : Invalid()
        public data class SignatureMismatch(override val sct: SignedCertificateTimestamp) : Invalid()
    }
}
