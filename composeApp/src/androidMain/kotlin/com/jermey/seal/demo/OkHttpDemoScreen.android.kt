package com.jermey.seal.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.seal.android.okhttp.installCertificateTransparency
import com.jermey.seal.core.model.VerificationResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Android-specific composable screen demonstrating OkHttp-based CT verification.
 */
@Composable
fun OkHttpDemoScreen() {
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
                    val client = OkHttpClient.Builder()
                        .installCertificateTransparency {
                            failOnError = false
                            logger = { host, result ->
                                android.util.Log.d("SealCT", "OkHttp CT [$host]: $result")
                                verificationResults[host] = result
                            }
                        }
                        .build()

                    results = results.map { checkResult ->
                        try {
                            android.util.Log.d("SealCT", "OkHttp: Starting CT check for ${checkResult.url}")
                            withContext(Dispatchers.IO) {
                                val request = Request.Builder()
                                    .url(checkResult.url)
                                    .build()
                                client.newCall(request).execute().close()
                            }
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
                            android.util.Log.e("SealCT", "OkHttp: Error checking ${checkResult.url}", e)
                            checkResult.copy(
                                status = CtCheckResult.Status.Error(
                                    e.message ?: "Unknown error"
                                )
                            )
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
