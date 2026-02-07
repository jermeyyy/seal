package com.jermey.seal.core.model

/**
 * Result of a Certificate Transparency verification for a TLS connection.
 *
 * Describes whether a certificate chain satisfies the configured [CTPolicy][com.jermey.seal.core.policy.CTPolicy]
 * or why verification was skipped/failed.
 *
 * @see SctVerificationResult
 */
public sealed class VerificationResult {

    /**
     * Verification completed successfully — the connection is considered CT-compliant
     * (or was explicitly skipped).
     */
    public sealed class Success : VerificationResult() {
        /**
         * The certificate chain has enough valid SCTs to satisfy the CT policy.
         *
         * @property validScts The SCTs that passed signature verification.
         */
        public data class Trusted(public val validScts: List<SctVerificationResult.Valid>) : Success()

        /** Verification was skipped because the connection is not secured (plain HTTP). */
        public data object InsecureConnection : Success()

        /** Verification was skipped because the host is excluded by the host matcher. */
        public data object DisabledForHost : Success()

        /** Verification was skipped because the log list is stale and the policy allows it. */
        public data object DisabledStaleLogList : Success()
    }

    /**
     * Verification failed — the connection does not satisfy the CT policy.
     */
    public sealed class Failure : VerificationResult() {
        /** No SCTs were found in the certificate, TLS extension, or OCSP response. */
        public data object NoScts : Failure()

        /**
         * Some SCTs were valid but fewer than the policy requires.
         *
         * @property found The number of valid SCTs found.
         * @property required The number of valid SCTs the policy requires.
         */
        public data class TooFewSctsTrusted(public val found: Int, public val required: Int) : Failure()

        /**
         * Valid SCTs exist but from too few distinct log operators.
         *
         * @property found The number of distinct operators found.
         * @property required The minimum number of distinct operators required.
         */
        public data class TooFewDistinctOperators(public val found: Int, public val required: Int) : Failure()

        /**
         * All SCTs failed verification against their respective log servers.
         *
         * @property sctResults The individual verification results for each SCT.
         */
        public data class LogServersFailed(public val sctResults: List<SctVerificationResult>) : Failure()

        /**
         * An unexpected error occurred during verification.
         *
         * @property cause The underlying exception.
         */
        public data class UnknownError(public val cause: Throwable) : Failure()
    }
}
