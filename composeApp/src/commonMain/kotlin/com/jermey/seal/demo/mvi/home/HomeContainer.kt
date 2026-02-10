package com.jermey.seal.demo.mvi.home

import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.TEST_URLS
import com.jermey.seal.demo.data.CtVerificationRepository
import com.jermey.seal.demo.data.VerificationResultDto
import com.jermey.quo.vadis.flowmvi.NavigationContainer
import com.jermey.quo.vadis.flowmvi.NavigationContainerScope
import pro.respawn.flowmvi.api.ActionShareBehavior
import pro.respawn.flowmvi.api.Store
import pro.respawn.flowmvi.dsl.store
import pro.respawn.flowmvi.plugins.reduce
import kotlinx.serialization.json.Json

class HomeContainer(
    private val repository: CtVerificationRepository,
    scope: NavigationContainerScope,
) : NavigationContainer<HomeState, HomeIntent, HomeAction>(scope) {

    override val store: Store<HomeState, HomeIntent, HomeAction> =
        store<HomeState, HomeIntent, HomeAction>(HomeState.initial(repository.availableEngines), coroutineScope) {
            configure {
                actionShareBehavior = ActionShareBehavior.Distribute()
            }
            reduce { intent ->
                when (intent) {
                    is HomeIntent.SelectEngine -> {
                        updateState {
                            copy(
                                selectedEngine = intent.engine,
                                results = emptyList(),
                                customUrlResult = null,
                            )
                        }
                    }
                    is HomeIntent.RunBatchTest -> {
                        var engine = HomeState.initial(emptyList()).selectedEngine
                        updateState {
                            engine = selectedEngine
                            copy(
                                isRunning = true,
                                results = TEST_URLS.map { CtCheckResult(it, CtCheckResult.Status.Loading) },
                            )
                        }
                        repository.verifyUrls(TEST_URLS, engine).collect { result ->
                            updateState {
                                copy(results = results.map { if (it.url == result.url) result else it })
                            }
                        }
                        updateState { copy(isRunning = false) }
                    }
                    is HomeIntent.UpdateCustomUrl -> {
                        updateState {
                            copy(
                                customUrl = intent.url,
                                customUrlError = if (customUrlError) false else customUrlError,
                            )
                        }
                    }
                    is HomeIntent.TestCustomUrl -> {
                        var urlToTest: String? = null
                        var engine = HomeState.initial(emptyList()).selectedEngine
                        updateState {
                            if (customUrl.isBlank()) {
                                return@updateState copy(customUrlError = true)
                            }
                            val normalized = if (customUrl.trim().startsWith("https://") || customUrl.trim().startsWith("http://")) {
                                customUrl.trim()
                            } else {
                                "https://${customUrl.trim()}"
                            }
                            urlToTest = normalized
                            engine = selectedEngine
                            copy(
                                isCheckingCustomUrl = true,
                                customUrlResult = CtCheckResult(normalized, CtCheckResult.Status.Loading),
                                customUrlError = false,
                            )
                        }
                        urlToTest?.let { url ->
                            val result = repository.verifyUrl(url, engine)
                            updateState {
                                copy(isCheckingCustomUrl = false, customUrlResult = result)
                            }
                        }
                    }
                    is HomeIntent.ResultClicked -> {
                        val status = intent.result.status
                        if (status is CtCheckResult.Status.Completed) {
                            val dto = VerificationResultDto.from(status.result)
                            val json = Json.encodeToString(VerificationResultDto.serializer(), dto)
                            action(HomeAction.NavigateToDetails(intent.result.url, json))
                        }
                    }
                    is HomeIntent.ToggleAbout -> {
                        updateState { copy(showAbout = !showAbout) }
                    }
                    is HomeIntent.DismissAbout -> {
                        updateState { copy(showAbout = false) }
                    }
                }
            }
        }
}
