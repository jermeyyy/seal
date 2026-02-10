package com.jermey.seal.core.policy

import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.VerificationResult

/**
 * Apple CT policy implementation.
 *
 * This policy mirrors Apple's Certificate Transparency requirements:
 * - Certificate lifetime ≤ 180 days → requires ≥ 2 valid SCTs
 * - Certificate lifetime > 180 days → requires ≥ 3 valid SCTs
 * - **Operator diversity:** at least 2 SCTs must be from distinct log operators (any operators).
 *
 * Unlike [ChromeCtPolicy], this policy does **not** require a Google-specific operator.
 * It only requires that SCTs come from at least 2 different operators, regardless of who they are.
 * This makes it more lenient for certificates using non-Google CT logs.
 *
 * @see ChromeCtPolicy
 */
public class AppleCtPolicy : CTPolicy {

    override fun evaluate(
        certificateLifetimeDays: Long,
        sctResults: List<SctVerificationResult>,
    ): VerificationResult {
        val validScts = sctResults.filterIsInstance<SctVerificationResult.Valid>()

        if (validScts.isEmpty()) {
            // Distinguish: no SCTs at all vs all SCTs failed
            return if (sctResults.isEmpty()) {
                VerificationResult.Failure.NoScts
            } else {
                VerificationResult.Failure.LogServersFailed(sctResults)
            }
        }

        val requiredScts = if (certificateLifetimeDays <= 180) 2 else 3

        if (validScts.size < requiredScts) {
            return VerificationResult.Failure.TooFewSctsTrusted(
                found = validScts.size,
                required = requiredScts,
            )
        }

        val distinctOperators = validScts
            .mapNotNull { it.logOperator }
            .distinct()
            .size

        if (distinctOperators < 2) {
            return VerificationResult.Failure.TooFewDistinctOperators(
                found = distinctOperators,
                required = 2,
            )
        }

        return VerificationResult.Success.Trusted(validScts)
    }
}
