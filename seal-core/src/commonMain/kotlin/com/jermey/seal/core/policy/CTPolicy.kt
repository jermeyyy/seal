package com.jermey.seal.core.policy

import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.VerificationResult

/**
 * Policy that determines whether a certificate's CT compliance is sufficient.
 * Implementations evaluate the number and quality of valid SCTs against policy requirements.
 */
public fun interface CTPolicy {
    /**
     * Evaluate whether the given SCT verification results satisfy this policy.
     * @param certificateLifetimeDays Validity period of the leaf certificate in days
     * @param sctResults Results of individual SCT verifications
     * @return VerificationResult indicating policy compliance
     */
    public fun evaluate(
        certificateLifetimeDays: Long,
        sctResults: List<SctVerificationResult>,
    ): VerificationResult
}
