package com.jermey.seal.demo.mvi.details

import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

data class DetailsState(
    val url: String,
    val isSuccess: Boolean,
    val statusTitle: String,
    val statusDescription: String,
    val sctDetails: List<SctDetail>,
    val policyCompliance: String,
) : MVIState {
    companion object {
        val Loading = DetailsState(
            url = "",
            isSuccess = false,
            statusTitle = "Loading…",
            statusDescription = "",
            sctDetails = emptyList(),
            policyCompliance = "",
        )
    }
}

data class SctDetail(
    val logIdHex: String,
    val timestamp: String,
    val origin: String,
    val operatorName: String,
    val signatureAlgorithm: String,
    val verificationStatus: String,
    val isValid: Boolean,
)

sealed interface DetailsIntent : MVIIntent {
    data object GoBack : DetailsIntent
}

sealed interface DetailsAction : MVIAction {
    data object NavigateBack : DetailsAction
}
