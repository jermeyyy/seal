package com.jermey.seal.core.model

public sealed class VerificationResult {
    public sealed class Success : VerificationResult() {
        public data class Trusted(public val validScts: List<SctVerificationResult.Valid>) : Success()
        public data object InsecureConnection : Success()
        public data object DisabledForHost : Success()
        public data object DisabledStaleLogList : Success()
    }

    public sealed class Failure : VerificationResult() {
        public data object NoScts : Failure()
        public data class TooFewSctsTrusted(public val found: Int, public val required: Int) : Failure()
        public data class TooFewDistinctOperators(public val found: Int, public val required: Int) : Failure()
        public data class LogServersFailed(public val sctResults: List<SctVerificationResult>) : Failure()
        public data class UnknownError(public val cause: Throwable) : Failure()
    }
}
