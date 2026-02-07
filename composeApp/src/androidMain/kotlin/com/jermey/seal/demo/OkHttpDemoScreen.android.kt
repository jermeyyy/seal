package com.jermey.seal.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.seal.android.okhttp.certificateTransparencyInterceptor
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
        Button(
            onClick = {
                isRunning = true
                verificationResults.clear()
                results = TEST_URLS.map { CtCheckResult(it, CtCheckResult.Status.Loading) }

                scope.launch {
                    val client = OkHttpClient.Builder()
                        .addNetworkInterceptor(
                            certificateTransparencyInterceptor {
                                failOnError = false
                                logger = { host, result ->
                                    verificationResults[host] = result
                                }
                            }
                        )
                        .build()

                    results = results.map { checkResult ->
                        try {
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        ) {
            if (isRunning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isRunning) "Running…" else "Run OkHttp CT Test")
        }

        if (isRunning) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

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
