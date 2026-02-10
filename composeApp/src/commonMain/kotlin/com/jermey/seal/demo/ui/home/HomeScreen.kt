package com.jermey.seal.demo.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.flowmvi.rememberContainer
import org.koin.core.qualifier.qualifier
import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.Engine
import com.jermey.seal.demo.mvi.home.HomeAction
import com.jermey.seal.demo.mvi.home.HomeContainer
import com.jermey.seal.demo.mvi.home.HomeIntent
import com.jermey.seal.demo.mvi.home.HomeState
import com.jermey.seal.demo.navigation.MainDestination
import com.jermey.seal.demo.ui.components.AppHeader
import com.jermey.seal.demo.ui.components.ResultCard
import com.jermey.seal.demo.ui.components.SummaryStatsBar
import org.jetbrains.compose.resources.painterResource
import pro.respawn.flowmvi.compose.dsl.subscribe
import seal.composeapp.generated.resources.Res
import seal.composeapp.generated.resources.logo

@Screen(MainDestination.Home::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navigator: Navigator) {
    val store = rememberContainer<HomeContainer, HomeState, HomeIntent, HomeAction>(
        qualifier = qualifier<HomeContainer>(),
    )
    val uriHandler = LocalUriHandler.current

    val state by store.subscribe { action ->
        when (action) {
            is HomeAction.NavigateToDetails -> navigator.navigate(
                MainDestination.Details(action.url, action.resultJson),
            )
            is HomeAction.OpenUri -> uriHandler.openUri(action.uri)
        }
    }

    // About dialog
    if (state.showAbout) {
        AlertDialog(
            onDismissRequest = { store.intent(HomeIntent.DismissAbout) },
            icon = {
                Image(
                    painter = painterResource(Res.drawable.logo),
                    contentDescription = "Seal Logo",
                    modifier = Modifier.size(48.dp),
                )
            },
            title = { Text("About Seal") },
            text = {
                Text(
                    "Certificate Transparency verification library for Kotlin Multiplatform." +
                        "\n\nCreated by jermeyyy",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.intent(HomeIntent.DismissAbout)
                    uriHandler.openUri("https://github.com/jermeyyy")
                }) {
                    Text("GitHub")
                }
            },
            dismissButton = {
                TextButton(onClick = { store.intent(HomeIntent.DismissAbout) }) {
                    Text("Close")
                }
            },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        AppHeader(onInfoClick = { store.intent(HomeIntent.ToggleAbout) })

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

        // Engine selector (visible only if multiple engines available)
        if (state.availableEngines.size > 1) {
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                state.availableEngines.forEachIndexed { index, engine ->
                    SegmentedButton(
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = state.availableEngines.size,
                        ),
                        onClick = { store.intent(HomeIntent.SelectEngine(engine)) },
                        selected = state.selectedEngine == engine,
                        icon = {
                            SegmentedButtonDefaults.Icon(active = state.selectedEngine == engine) {
                                Icon(
                                    imageVector = when (engine) {
                                        Engine.Ktor -> Icons.Default.Language
                                        Engine.OkHttp -> Icons.Default.Http
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        },
                    ) {
                        Text(engine.title, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        // Custom URL section
        Text(
            text = "Test Custom URL",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            OutlinedTextField(
                value = state.customUrl,
                onValueChange = { store.intent(HomeIntent.UpdateCustomUrl(it)) },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter URL to test") },
                leadingIcon = { Icon(Icons.Default.Link, contentDescription = null) },
                isError = state.customUrlError,
                supportingText = if (state.customUrlError) {
                    { Text("Please enter a URL") }
                } else {
                    null
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = { store.intent(HomeIntent.TestCustomUrl) }),
            )
            Spacer(modifier = Modifier.width(8.dp))
            FilledIconButton(
                onClick = { store.intent(HomeIntent.TestCustomUrl) },
                enabled = !state.isCheckingCustomUrl,
                modifier = Modifier.padding(top = 4.dp).size(48.dp),
            ) {
                if (state.isCheckingCustomUrl) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Test URL")
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Run button
        Button(
            onClick = { store.intent(HomeIntent.RunBatchTest) },
            enabled = !state.isRunning,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(if (state.isRunning) "Running…" else "Run CT Test")
        }

        if (state.isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Summary stats
        val allResults = buildList {
            state.customUrlResult?.let { add(it) }
            addAll(state.results)
        }
        val hasResults = allResults.any {
            it.status is CtCheckResult.Status.Completed || it.status is CtCheckResult.Status.Error
        }
        if (hasResults) {
            SummaryStatsBar(allResults)
        }

        // Results list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            state.customUrlResult?.let { customResult ->
                item(key = "custom_url_result") {
                    ResultCard(
                        result = customResult,
                        onClick = { store.intent(HomeIntent.ResultClicked(customResult)) },
                    )
                }
            }
            items(state.results, key = { it.url }) { result ->
                ResultCard(
                    result = result,
                    onClick = { store.intent(HomeIntent.ResultClicked(result)) },
                )
            }
        }
    }
}
