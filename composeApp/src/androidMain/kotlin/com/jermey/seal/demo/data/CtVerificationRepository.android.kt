package com.jermey.seal.demo.data

import com.jermey.seal.android.okhttp.installCertificateTransparency
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.Engine
import com.jermey.seal.demo.createCtHttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

actual class CtVerificationRepository actual constructor() {

    actual val availableEngines: List<Engine> = listOf(Engine.Ktor, Engine.OkHttp)

    actual fun verifyUrls(urls: List<String>, engine: Engine): Flow<CtCheckResult> = flow {
        for (url in urls) {
            emit(verifyUrl(url, engine))
        }
    }

    actual suspend fun verifyUrl(url: String, engine: Engine): CtCheckResult {
        val normalizedUrl = normalizeUrl(url)
        return try {
            when (engine) {
                Engine.Ktor -> verifyWithKtor(normalizedUrl)
                Engine.OkHttp -> verifyWithOkHttp(normalizedUrl)
            }
        } catch (e: Exception) {
            CtCheckResult(normalizedUrl, CtCheckResult.Status.Error(e.message ?: "Unknown error"))
        }
    }

    private suspend fun verifyWithKtor(url: String): CtCheckResult {
        val verificationResults = mutableMapOf<String, VerificationResult>()
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
            httpClient.get(url)
        }
        // Try original host first, then fall back to any captured result.
        // This handles redirects where the TLS handshake host differs from the original URL host.
        val host = extractHost(url)
        val vr = verificationResults[host]
            ?: verificationResults.values.lastOrNull()
        return if (vr != null) {
            CtCheckResult(url, CtCheckResult.Status.Completed(vr))
        } else {
            CtCheckResult(url, CtCheckResult.Status.Completed(VerificationResult.Success.DisabledForHost))
        }
    }

    private suspend fun verifyWithOkHttp(url: String): CtCheckResult {
        val verificationResults = mutableMapOf<String, VerificationResult>()
        val client = OkHttpClient.Builder()
            .installCertificateTransparency {
                failOnError = false
                logger = { host, result ->
                    android.util.Log.d("SealCT", "OkHttp CT [$host]: $result")
                    verificationResults[host] = result
                }
            }
            .build()
        withContext(Dispatchers.IO) {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().close()
        }
        // Try original host first, then fall back to any captured result.
        // This handles redirects where the TLS handshake host differs from the original URL host.
        val host = extractHost(url)
        val vr = verificationResults[host]
            ?: verificationResults.values.lastOrNull()
        return if (vr != null) {
            CtCheckResult(url, CtCheckResult.Status.Completed(vr))
        } else {
            CtCheckResult(url, CtCheckResult.Status.Completed(VerificationResult.Success.DisabledForHost))
        }
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    private fun extractHost(url: String): String =
        url.removePrefix("https://").removePrefix("http://")
            .substringBefore('/')
            .substringBefore(':')
}
