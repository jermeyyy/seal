package com.jermey.seal.demo.data

import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.Engine
import com.jermey.seal.demo.createCtHttpClient
import io.ktor.client.request.get
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

actual class CtVerificationRepository actual constructor() {

    actual val availableEngines: List<Engine> = listOf(Engine.Ktor)

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
                Engine.OkHttp -> throw UnsupportedOperationException("OkHttp is not available in browser")
            }
        } catch (e: UnsupportedOperationException) {
            throw e
        } catch (e: Exception) {
            CtCheckResult(normalizedUrl, CtCheckResult.Status.Error(e.message ?: "Unknown error"))
        }
    }

    private suspend fun verifyWithKtor(url: String): CtCheckResult {
        val proxyUrl = url.toProxyUrl()
        val client = createCtHttpClient(httpConfig = {
            followRedirects = false
        })
        return try {
            client.use { httpClient ->
                httpClient.get(proxyUrl)
            }
            CtCheckResult(
                url,
                CtCheckResult.Status.Completed(VerificationResult.Success.DisabledForHost),
            )
        } catch (e: Exception) {
            CtCheckResult(
                url,
                CtCheckResult.Status.Error(e.message ?: "Unknown error"),
            )
        }
    }

    private fun String.toProxyUrl(): String {
        val withoutScheme = removePrefix("https://").removePrefix("http://")
        return "/ct-proxy/$withoutScheme"
    }

    private fun normalizeUrl(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("https://") || trimmed.startsWith("http://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }
}
