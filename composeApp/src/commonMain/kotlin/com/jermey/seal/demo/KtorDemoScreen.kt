package com.jermey.seal.demo

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jermey.seal.core.model.VerificationResult
import io.ktor.client.request.get
import kotlinx.coroutines.launch

/**
 * Composable screen that demonstrates Ktor-based CT verification.
 * Works on both Android and iOS platforms.
 */
@Composable
fun KtorDemoScreen() {
    var results by remember {
        mutableStateOf(TEST_URLS.map { CtCheckResult(it, CtCheckResult.Status.Pending) })
    }
    var isRunning by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Captured verification results from the logger callback
    val verificationResults = remember { mutableMapOf<String, VerificationResult>() }

    Column(modifier = Modifier.fillMaxSize()) {
        // Run button
        Button(
            onClick = {
                isRunning = true
                verificationResults.clear()
                results = TEST_URLS.map { CtCheckResult(it, CtCheckResult.Status.Loading) }

                scope.launch {
                    val client = createCtHttpClient(
                        ctConfig = {
                            failOnError = false
                            logger = { host, result ->
                                println("SealCT Ktor CT [$host]: $result")
                                verificationResults[host] = result
                            }
                        },
                    )

                    client.use { httpClient ->
                        results = results.map { checkResult ->
                            try {
                                println("SealCT Ktor: Starting CT check for ${checkResult.url}")
                                httpClient.get(checkResult.url)
                                val host = checkResult.url
                                    .removePrefix("https://")
                                    .removePrefix("http://")
                                    .trimEnd('/')
                                val vr = verificationResults[host]
                                if (vr != null) {
                                    checkResult.copy(status = CtCheckResult.Status.Completed(vr))
                                } else {
                                    checkResult.copy(
                                        status = CtCheckResult.Status.Completed(
                                            VerificationResult.Success.DisabledForHost
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                println("SealCT Ktor: Error checking ${checkResult.url}: ${e.message}")
                                checkResult.copy(
                                    status = CtCheckResult.Status.Error(
                                        e.message ?: "Unknown error"
                                    )
                                )
                            }
                        }
                    }
                    isRunning = false
                }
            },
            enabled = !isRunning,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            shape = MaterialTheme.shapes.medium,
        ) {
            if (isRunning) {
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
            Text(if (isRunning) "Running…" else "Run CT Test")
        }

        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Summary stats
        val hasResults = results.any { it.status is CtCheckResult.Status.Completed || it.status is CtCheckResult.Status.Error }
        if (hasResults) {
            SummaryStatsBar(results)
        }

        // Results list
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(results, key = { it.url }) { result ->
                ResultCard(result)
            }
        }
    }
}

/**
 * A compact stats bar showing pass/fail/error counts after test completion.
 */
@Composable
fun SummaryStatsBar(results: List<CtCheckResult>) {
    val passed = results.count { check ->
        val s = check.status
        s is CtCheckResult.Status.Completed && s.result is VerificationResult.Success
    }
    val failed = results.count { check ->
        val s = check.status
        (s is CtCheckResult.Status.Completed && s.result is VerificationResult.Failure) ||
            s is CtCheckResult.Status.Error
    }
    val pending = results.size - passed - failed

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().animateContentSize(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            StatItem(
                icon = Icons.Default.CheckCircle,
                label = "Passed",
                count = passed,
                tint = Color(0xFF2E7D32),
            )
            StatItem(
                icon = Icons.Default.Cancel,
                label = "Failed",
                count = failed,
                tint = MaterialTheme.colorScheme.error,
            )
            if (pending > 0) {
                StatItem(
                    icon = Icons.Default.PlayArrow,
                    label = "Pending",
                    count = pending,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    count: Int,
    tint: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "$count",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tint,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
