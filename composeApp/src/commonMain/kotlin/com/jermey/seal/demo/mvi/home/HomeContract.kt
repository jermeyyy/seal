package com.jermey.seal.demo.mvi.home

import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.Engine
import pro.respawn.flowmvi.api.MVIAction
import pro.respawn.flowmvi.api.MVIIntent
import pro.respawn.flowmvi.api.MVIState

data class HomeState(
    val selectedEngine: Engine,
    val availableEngines: List<Engine>,
    val isRunning: Boolean,
    val results: List<CtCheckResult>,
    val customUrl: String,
    val customUrlError: Boolean,
    val customUrlResult: CtCheckResult?,
    val isCheckingCustomUrl: Boolean,
    val showAbout: Boolean,
) : MVIState {
    companion object {
        fun initial(availableEngines: List<Engine>) = HomeState(
            selectedEngine = Engine.Ktor,
            availableEngines = availableEngines,
            isRunning = false,
            results = emptyList(),
            customUrl = "",
            customUrlError = false,
            customUrlResult = null,
            isCheckingCustomUrl = false,
            showAbout = false,
        )
    }
}

sealed interface HomeIntent : MVIIntent {
    data class SelectEngine(val engine: Engine) : HomeIntent
    data object RunBatchTest : HomeIntent
    data class UpdateCustomUrl(val url: String) : HomeIntent
    data object TestCustomUrl : HomeIntent
    data class ResultClicked(val result: CtCheckResult) : HomeIntent
    data object ToggleAbout : HomeIntent
    data object DismissAbout : HomeIntent
}

sealed interface HomeAction : MVIAction {
    data class NavigateToDetails(val url: String, val resultJson: String) : HomeAction
    data class OpenUri(val uri: String) : HomeAction
}
