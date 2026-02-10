package com.jermey.seal.core.policy

import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.VerificationResult

/**
 * Chrome CT policy implementation.
 *
 * This policy mirrors Chrome's Certificate Transparency requirements:
 * - Certificate lifetime < 180 days → requires ≥ 2 valid SCTs
 * - Certificate lifetime ≥ 180 days → requires ≥ 3 valid SCTs
 * - **Operator diversity:** at least 1 SCT must come from a Google-operated log
 *   AND at least 1 from a non-Google log.
 *
 * **Important:** The Google-specific operator diversity requirement means that certificates
 * with valid SCTs from multiple distinct operators—but none named "Google"—will fail.
 * This can happen for certificates that use only Let's Encrypt, DigiCert, or other
 * non-Google CT logs.
 *
 * If you don't need Google-specific diversity, consider using [AppleCtPolicy] instead,
 * which requires only 2 distinct operators (of any type).
 *
 * @see AppleCtPolicy
 */
public class ChromeCtPolicy : CTPolicy {

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

        val requiredScts = if (certificateLifetimeDays < 180) 2 else 3

        if (validScts.size < requiredScts) {
            return VerificationResult.Failure.TooFewSctsTrusted(
                found = validScts.size,
                required = requiredScts,
            )
        }

        val hasGoogle = validScts.any { it.logOperator.isGoogle() }
        val hasNonGoogle = validScts.any { !it.logOperator.isGoogle() }

        if (!hasGoogle || !hasNonGoogle) {
            return VerificationResult.Failure.TooFewDistinctOperators(
                found = if (hasGoogle || hasNonGoogle) 1 else 0,
                required = 2,
            )
        }

        return VerificationResult.Success.Trusted(validScts)
    }

    private fun String?.isGoogle(): Boolean =
        this != null && contains("Google", ignoreCase = true)
}
