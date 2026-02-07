package com.jermey.seal.demo

import com.jermey.seal.core.model.VerificationResult

/**
 * Represents the result of a CT verification check for a single URL.
 */
data class CtCheckResult(
    val url: String,
    val status: Status,
) {
    sealed class Status {
        data object Pending : Status()
        data object Loading : Status()
        data class Completed(val result: VerificationResult) : Status()
        data class Error(val message: String) : Status()
    }
}

val TEST_URLS = listOf(
    "https://www.google.com",
    "https://github.com",
    "https://www.cloudflare.com",
    "https://www.facebook.com",
    "https://www.amazon.com",
)
