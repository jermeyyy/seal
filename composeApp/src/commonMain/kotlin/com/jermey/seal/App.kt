package com.jermey.seal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jermey.seal.demo.DemoTab
import com.jermey.seal.demo.KtorDemoScreen
import com.jermey.seal.demo.isAndroid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    MaterialTheme {
        val availableTabs = remember {
            if (isAndroid) DemoTab.entries else listOf(DemoTab.Ktor)
        }
        var selectedTab by remember { mutableStateOf(availableTabs.first()) }

        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Seal CT Demo") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                    ),
                )
            }
        ) { padding ->
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                if (availableTabs.size > 1) {
                    PrimaryTabRow(selectedTabIndex = availableTabs.indexOf(selectedTab)) {
                        availableTabs.forEach { tab ->
                            Tab(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                text = { Text(tab.title) },
                            )
                        }
                    }
                }

                when (selectedTab) {
                    DemoTab.Ktor -> KtorDemoScreen()
                    DemoTab.OkHttp -> OkHttpDemoContent()
                }
            }
        }
    }
}

/**
 * Platform-resolved composable for the OkHttp demo tab.
 * On Android this renders the OkHttp demo screen; it should never be
 * called on iOS because the tab is hidden.
 */
@Composable
expect fun OkHttpDemoContent()