package com.jermey.seal.demo.mvi.details

import com.jermey.seal.demo.data.VerificationResultDto
import com.jermey.seal.demo.navigation.MainDestination
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import pro.respawn.flowmvi.api.ActionShareBehavior
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json

class DetailsContainer(
    scope: NavigationContainerScope,
) : NavigationContainer<DetailsState, DetailsIntent, DetailsAction>(scope) {

    private val destination = scope.screenNode.destination as MainDestination.Details
    private val json = Json { ignoreUnknownKeys = true }

    override val store: Store<DetailsState, DetailsIntent, DetailsAction> =
        store<DetailsState, DetailsIntent, DetailsAction>(parseState(), coroutineScope) {
        configure {
            actionShareBehavior = ActionShareBehavior.Distribute()
        }
        reduce { intent ->
            when (intent) {
                is DetailsIntent.GoBack -> action(DetailsAction.NavigateBack)
            }
        }
    }

    private fun parseState(): DetailsState {
        return try {
            val dto = json.decodeFromString(VerificationResultDto.serializer(), destination.resultJson)
            DetailsState(
                url = destination.url,
                isSuccess = dto.isSuccess,
                statusTitle = when (dto.type) {
                    "Trusted" -> "CT Verified"
                    "InsecureConnection" -> "Insecure Connection"
                    "DisabledForHost" -> "CT Disabled"
                    "DisabledStaleLogList" -> "CT Disabled (Stale Log List)"
                    "NoScts" -> "No SCTs Found"
                    "TooFewSctsTrusted" -> "Too Few Trusted SCTs"
                    "TooFewDistinctOperators" -> "Too Few Distinct Operators"
                    "LogServersFailed" -> "Log Server Verification Failed"
                    "UnknownError" -> "Verification Error"
                    else -> "Unknown"
                },
                statusDescription = when (dto.type) {
                    "Trusted" -> "${dto.validScts.size} valid SCTs"
                    "InsecureConnection" -> "Connection is not secured (plain HTTP)"
                    "DisabledForHost" -> "CT verification disabled for this host"
                    "DisabledStaleLogList" -> "CT verification disabled due to stale log list"
                    "NoScts" -> "No Signed Certificate Timestamps found"
                    "TooFewSctsTrusted" -> "${dto.found ?: 0} of ${dto.required ?: 0} required SCTs trusted"
                    "TooFewDistinctOperators" -> "${dto.found ?: 0} of ${dto.required ?: 0} required distinct operators"
                    "LogServersFailed" -> "All SCTs failed verification against log servers"
                    "UnknownError" -> dto.errorMessage ?: "Unknown error"
                    else -> ""
                },
                sctDetails = (dto.validScts + dto.invalidScts).map { sctDto ->
                    SctDetail(
                        logIdHex = sctDto.logIdHex,
                        timestamp = formatTimestamp(sctDto.timestampMs),
                        origin = sctDto.origin,
                        operatorName = sctDto.operatorName ?: "Unknown",
                        signatureAlgorithm = "${sctDto.hashAlgorithm} with ${sctDto.signatureAlgorithm}",
                        verificationStatus = if (sctDto.isValid) "Valid" else "Invalid — ${sctDto.invalidReason ?: "Unknown reason"}",
                        isValid = sctDto.isValid,
                    )
                },
                policyCompliance = when {
                    dto.type == "Trusted" -> "Compliant — Chrome CT Policy satisfied"
                    dto.isSuccess -> "Compliant — verification passed"
                    else -> "Non-compliant — CT Policy not satisfied"
                },
            )
        } catch (e: Exception) {
            DetailsState(
                url = destination.url,
                isSuccess = false,
                statusTitle = "Parse Error",
                statusDescription = "Failed to parse verification result: ${e.message}",
                sctDetails = emptyList(),
                policyCompliance = "",
            )
        }
    }

    private fun formatTimestamp(epochMs: Long): String {
        val instant = Instant.fromEpochMilliseconds(epochMs)
        val dt = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return "${dt.year}-${dt.monthNumber.toString().padStart(2, '0')}-${dt.dayOfMonth.toString().padStart(2, '0')} " +
            "${dt.hour.toString().padStart(2, '0')}:${dt.minute.toString().padStart(2, '0')}:${dt.second.toString().padStart(2, '0')}"
    }
}
