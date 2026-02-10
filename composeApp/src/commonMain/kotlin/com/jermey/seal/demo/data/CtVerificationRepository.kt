package com.jermey.seal.demo.data

import com.jermey.seal.demo.CtCheckResult
import com.jermey.seal.demo.Engine
import kotlinx.coroutines.flow.Flow

expect class CtVerificationRepository() {
    /**
     * Verify a batch of URLs sequentially, emitting each result as it completes.
     */
    fun verifyUrls(urls: List<String>, engine: Engine): Flow<CtCheckResult>

    /**
     * Verify a single URL and return the result.
     */
    suspend fun verifyUrl(url: String, engine: Engine): CtCheckResult

    /**
     * Available engines on this platform.
     */
    val availableEngines: List<Engine>
}
