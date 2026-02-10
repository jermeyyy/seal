package com.jermey.seal.demo.ui.details

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jermey.quo.vadis.annotations.Screen
import com.jermey.quo.vadis.core.navigation.navigator.Navigator
import com.jermey.quo.vadis.flowmvi.rememberContainer
import org.koin.core.qualifier.qualifier
import com.jermey.seal.demo.navigation.MainDestination
import com.jermey.seal.demo.mvi.details.DetailsAction
import com.jermey.seal.demo.mvi.details.DetailsContainer
import com.jermey.seal.demo.mvi.details.DetailsIntent
import com.jermey.seal.demo.mvi.details.DetailsState
import com.jermey.seal.demo.mvi.details.SctDetail
import pro.respawn.flowmvi.compose.dsl.subscribe

@Screen(MainDestination.Details::class)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(navigator: Navigator) {
    val store = rememberContainer<DetailsContainer, DetailsState, DetailsIntent, DetailsAction>(
        qualifier = qualifier<DetailsContainer>(),
    )

    val state by store.subscribe { action ->
        when (action) {
            is DetailsAction.NavigateBack -> navigator.navigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CT Details") },
                navigationIcon = {
                    IconButton(onClick = { store.intent(DetailsIntent.GoBack) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (state.isSuccess) {
                            Color(0xFF2E7D32).copy(alpha = 0.1f)
                        } else {
                            MaterialTheme.colorScheme.errorContainer
                        },
                    ),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            imageVector = if (state.isSuccess) Icons.Default.CheckCircle else Icons.Default.Cancel,
                            contentDescription = null,
                            tint = if (state.isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = state.statusTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (state.isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            )
                            if (state.statusDescription.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = state.statusDescription,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // Connection Info
            item {
                Text(
                    text = "Connection Info",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "URL",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = state.url,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            // SCT Details (only if non-empty)
            if (state.sctDetails.isNotEmpty()) {
                item {
                    Text(
                        text = "SCT Details (${state.sctDetails.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
                items(state.sctDetails) { sct ->
                    SctDetailCard(sct)
                }
            }

            // Policy Compliance
            if (state.policyCompliance.isNotBlank()) {
                item {
                    Text(
                        text = "Policy Compliance",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(8.dp))
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = if (state.isSuccess) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = state.policyCompliance,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SctDetailCard(sct: SctDetail) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Verification status chip
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (sct.isValid) {
                        Color(0xFF2E7D32).copy(alpha = 0.12f)
                    } else {
                        MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
                    },
                ) {
                    Text(
                        text = sct.verificationStatus,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sct.isValid) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            DetailRow("Log ID", sct.logIdHex, monospace = true)
            DetailRow("Timestamp", sct.timestamp)
            DetailRow("Origin", sct.origin)
            DetailRow("Operator", sct.operatorName)
            DetailRow("Signature", sct.signatureAlgorithm)
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    monospace: Boolean = false,
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = if (monospace) FontFamily.Monospace else FontFamily.Default,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
