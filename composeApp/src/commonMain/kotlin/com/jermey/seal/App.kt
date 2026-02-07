package com.jermey.seal

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Http
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.seal.demo.DemoTab
import com.jermey.seal.demo.KtorDemoScreen
import com.jermey.seal.demo.SealTheme
import com.jermey.seal.demo.isAndroid
import org.jetbrains.compose.resources.painterResource
import seal.composeapp.generated.resources.Res
import seal.composeapp.generated.resources.logo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    SealTheme {
        var selectedEngine by remember {
            mutableStateOf(DemoTab.Ktor)
        }
        var showAbout by remember { mutableStateOf(false) }
        val uriHandler = LocalUriHandler.current

        if (showAbout) {
            AlertDialog(
                onDismissRequest = { showAbout = false },
                icon = {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "Seal Logo",
                        modifier = Modifier.size(48.dp),
                    )
                },
                title = {
                    Text("About Seal")
                },
                text = {
                    Text(
                        "Certificate Transparency verification library for Kotlin Multiplatform." +
                            "\n\nCreated by jermeyyy",
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        uriHandler.openUri("https://github.com/jermeyyy")
                    }) {
                        Text("GitHub")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAbout = false }) {
                        Text("Close")
                    }
                },
            )
        }

        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)) {
            // Main content column
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(Res.drawable.logo),
                        contentDescription = "Seal Logo",
                        modifier = Modifier.size(120.dp),
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Seal",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Certificate Transparency",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Verify CT compliance of HTTPS connections",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Engine selector (Android only — has both Ktor and OkHttp)
                if (isAndroid) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        DemoTab.entries.forEachIndexed { index, tab ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = DemoTab.entries.size,
                                ),
                                onClick = { selectedEngine = tab },
                                selected = selectedEngine == tab,
                                icon = {
                                    SegmentedButtonDefaults.Icon(active = selectedEngine == tab) {
                                        Icon(
                                            imageVector = when (tab) {
                                                DemoTab.Ktor -> Icons.Default.Language
                                                DemoTab.OkHttp -> Icons.Default.Http
                                            },
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                    }
                                },
                            ) {
                                Text(tab.title, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }

                // Content
                when (selectedEngine) {
                    DemoTab.Ktor -> KtorDemoScreen()
                    DemoTab.OkHttp -> OkHttpDemoContent()
                }
            }

            // Info button floating at top-right
            IconButton(
                onClick = { showAbout = true },
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = "About",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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