package com.jermey.seal.demo.data

import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.model.SctVerificationResult
import com.jermey.seal.core.model.Origin
import kotlinx.serialization.Serializable

@Serializable
data class VerificationResultDto(
    val type: String,
    val isSuccess: Boolean,
    val validScts: List<SctDto> = emptyList(),
    val invalidScts: List<SctDto> = emptyList(),
    val found: Int? = null,
    val required: Int? = null,
    val errorMessage: String? = null,
    val platformInfo: String? = null,
) {
    companion object {
        fun from(result: VerificationResult): VerificationResultDto = when (result) {
            is VerificationResult.Success.Trusted -> VerificationResultDto(
                type = "Trusted",
                isSuccess = true,
                validScts = result.validScts.map { SctDto.from(it) },
            )
            is VerificationResult.Success.OsVerified -> VerificationResultDto(
                type = "OsVerified",
                isSuccess = true,
                errorMessage = result.coreVerificationResult?.let { "Core: $it" },
                platformInfo = result.platform,
            )
            is VerificationResult.Success.InsecureConnection -> VerificationResultDto(
                type = "InsecureConnection",
                isSuccess = true,
            )
            is VerificationResult.Success.DisabledForHost -> VerificationResultDto(
                type = "DisabledForHost",
                isSuccess = true,
            )
            is VerificationResult.Success.DisabledStaleLogList -> VerificationResultDto(
                type = "DisabledStaleLogList",
                isSuccess = true,
            )
            is VerificationResult.Failure.NoScts -> VerificationResultDto(
                type = "NoScts",
                isSuccess = false,
            )
            is VerificationResult.Failure.TooFewSctsTrusted -> VerificationResultDto(
                type = "TooFewSctsTrusted",
                isSuccess = false,
                found = result.found,
                required = result.required,
            )
            is VerificationResult.Failure.TooFewDistinctOperators -> VerificationResultDto(
                type = "TooFewDistinctOperators",
                isSuccess = false,
                found = result.found,
                required = result.required,
            )
            is VerificationResult.Failure.LogServersFailed -> VerificationResultDto(
                type = "LogServersFailed",
                isSuccess = false,
                invalidScts = result.sctResults.filterIsInstance<SctVerificationResult.Invalid>().map { SctDto.fromInvalid(it) },
                validScts = result.sctResults.filterIsInstance<SctVerificationResult.Valid>().map { SctDto.from(it) },
            )
            is VerificationResult.Failure.UnknownError -> VerificationResultDto(
                type = "UnknownError",
                isSuccess = false,
                errorMessage = result.cause.message ?: "Unknown error",
            )
        }
    }
}

@Serializable
data class SctDto(
    val logIdHex: String,
    val timestampMs: Long,
    val origin: String,
    val operatorName: String?,
    val hashAlgorithm: String,
    val signatureAlgorithm: String,
    val isValid: Boolean,
    val invalidReason: String? = null,
) {
    companion object {
        fun from(valid: SctVerificationResult.Valid): SctDto {
            val sct = valid.sct
            return SctDto(
                logIdHex = sct.logId.keyId.joinToString(":") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') },
                timestampMs = sct.timestamp.toEpochMilliseconds(),
                origin = when (sct.origin) {
                    Origin.EMBEDDED -> "Embedded"
                    Origin.TLS_EXTENSION -> "TLS Extension"
                    Origin.OCSP_RESPONSE -> "OCSP Response"
                },
                operatorName = valid.logOperator,
                hashAlgorithm = sct.signature.hashAlgorithm.name,
                signatureAlgorithm = sct.signature.signatureAlgorithm.name,
                isValid = true,
            )
        }

        fun fromInvalid(invalid: SctVerificationResult.Invalid): SctDto {
            val sct = invalid.sct
            return SctDto(
                logIdHex = sct.logId.keyId.joinToString(":") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') },
                timestampMs = sct.timestamp.toEpochMilliseconds(),
                origin = when (sct.origin) {
                    Origin.EMBEDDED -> "Embedded"
                    Origin.TLS_EXTENSION -> "TLS Extension"
                    Origin.OCSP_RESPONSE -> "OCSP Response"
                },
                operatorName = null,
                hashAlgorithm = sct.signature.hashAlgorithm.name,
                signatureAlgorithm = sct.signature.signatureAlgorithm.name,
                isValid = false,
                invalidReason = when (invalid) {
                    is SctVerificationResult.Invalid.FailedVerification -> "Verification failed"
                    is SctVerificationResult.Invalid.LogNotTrusted -> "Log not trusted"
                    is SctVerificationResult.Invalid.LogExpired -> "Log expired"
                    is SctVerificationResult.Invalid.LogRejected -> "Log rejected"
                    is SctVerificationResult.Invalid.SignatureMismatch -> "Signature mismatch"
                },
            )
        }
    }
}
