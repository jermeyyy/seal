package com.jermey.seal.demo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.ktor.CertificateTransparency
import io.ktor.client.HttpClient
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
        Button(
            onClick = {
                isRunning = true
                verificationResults.clear()
                results = TEST_URLS.map { CtCheckResult(it, CtCheckResult.Status.Loading) }

                scope.launch {
                    val client = HttpClient(createPlatformHttpEngine()) {
                        install(CertificateTransparency) {
                            failOnError = false
                            logger = { host, result ->
                                verificationResults[host] = result
                            }
                        }
                    }

                    client.use { httpClient ->
                        results = results.map { checkResult ->
                            try {
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
            Text(if (isRunning) "Running…" else "Run Ktor CT Test")
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
