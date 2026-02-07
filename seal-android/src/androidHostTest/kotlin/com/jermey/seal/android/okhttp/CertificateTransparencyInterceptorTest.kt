package com.jermey.seal.android.okhttp

import com.jermey.seal.core.config.ctConfiguration
import com.jermey.seal.core.model.VerificationResult
import okhttp3.Call
import okhttp3.Connection
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CertificateTransparencyInterceptorTest {

    // region Test doubles

    /**
     * Minimal fake for [Interceptor.Chain] that returns canned responses.
     * [fakeConnection] defaults to `null`, simulating a non-network (application) interceptor.
     */
    private class FakeChain(
        private val request: Request,
        private val response: Response,
        private val fakeConnection: Connection? = null,
    ) : Interceptor.Chain {
        var proceededRequest: Request? = null

        override fun request(): Request = request

        override fun proceed(request: Request): Response {
            proceededRequest = request
            return response
        }

        override fun connection(): Connection? = fakeConnection

        override fun call(): Call = throw UnsupportedOperationException("Not used in tests")
        override fun connectTimeoutMillis(): Int = 10_000
        override fun withConnectTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun readTimeoutMillis(): Int = 10_000
        override fun withReadTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
        override fun writeTimeoutMillis(): Int = 10_000
        override fun withWriteTimeout(timeout: Int, unit: TimeUnit): Interceptor.Chain = this
    }

    // endregion

    // region Helpers

    private fun buildRequest(url: String = "https://example.com/"): Request =
        Request.Builder().url(url).build()

    private fun buildResponse(request: Request): Response =
        Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_2)
            .code(200)
            .message("OK")
            .body("".toResponseBody())
            .build()

    // endregion

    // region Tests

    @Test
    fun `non-matching host skips verification and logs DisabledForHost`() {
        val logged = mutableListOf<Pair<String, VerificationResult>>()

        val config = ctConfiguration {
            // Only include *.other.com — example.com should NOT match
            +"*.other.com"
            logger = { host, result -> logged.add(host to result) }
        }

        val interceptor = certificateTransparencyInterceptor {
            +"*.other.com"
            logger = { host, result -> logged.add(host to result) }
        }

        val request = buildRequest("https://example.com/api")
        val response = buildResponse(request)
        val chain = FakeChain(request, response)

        val result = interceptor.intercept(chain)

        // Response must pass through unchanged
        assertEquals(200, result.code)
        // Logger must have been called with DisabledForHost
        assertTrue(logged.isNotEmpty(), "Logger should have been invoked")
        assertIs<VerificationResult.Success.DisabledForHost>(logged.last().second)
        assertEquals("example.com", logged.last().first)
    }

    @Test
    fun `certificateTransparencyInterceptor DSL creates valid interceptor`() {
        val interceptor = certificateTransparencyInterceptor {
            +"*.example.com"
            -"internal.example.com"
            failOnError = false
        }

        assertNotNull(interceptor, "DSL should produce a non-null Interceptor")
        assertIs<Interceptor>(interceptor)
    }

    @Test
    fun `null connection returns response as-is`() {
        // When connection() returns null the interceptor should return the response untouched.
        val interceptor = certificateTransparencyInterceptor {
            +"*.example.com"
        }

        val request = buildRequest("https://example.com/")
        val response = buildResponse(request)
        // FakeChain with default null connection
        val chain = FakeChain(request, response)

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
    }

    @Test
    fun `logger receives DisabledForHost for excluded host`() {
        val logged = mutableListOf<Pair<String, VerificationResult>>()

        val interceptor = certificateTransparencyInterceptor {
            // Include everything, then exclude a specific host
            +"*"
            -"excluded.example.com"
            logger = { host, result -> logged.add(host to result) }
        }

        val request = buildRequest("https://excluded.example.com/path")
        val response = buildResponse(request)
        val chain = FakeChain(request, response)

        interceptor.intercept(chain)

        assertTrue(logged.isNotEmpty(), "Logger should have been invoked")
        assertEquals("excluded.example.com", logged.last().first)
        assertIs<VerificationResult.Success.DisabledForHost>(logged.last().second)
    }

    @Test
    fun `failOnError configuration is passed correctly`() {
        // Verify that the interceptor created via DSL respects failOnError.
        // With null connection, the interceptor returns early before any failure path,
        // but we can at least verify the interceptor is created and functional.
        val interceptor = certificateTransparencyInterceptor {
            failOnError = true
        }

        val request = buildRequest("https://example.com/")
        val response = buildResponse(request)
        val chain = FakeChain(request, response)

        // Should not throw — connection is null so verification is skipped
        val result = interceptor.intercept(chain)
        assertEquals(200, result.code)
    }

    @Test
    fun `empty includes matches all hosts`() {
        // When no includes are specified, the default HostMatcher matches everything.
        // With null connection the interceptor returns early, but it should NOT log
        // DisabledForHost — the host IS matched, just no TLS info available.
        val logged = mutableListOf<Pair<String, VerificationResult>>()

        val interceptor = certificateTransparencyInterceptor {
            // No includes and no excludes — default behavior
            logger = { host, result -> logged.add(host to result) }
        }

        val request = buildRequest("https://any-random-host.io/")
        val response = buildResponse(request)
        val chain = FakeChain(request, response)

        val result = interceptor.intercept(chain)

        assertEquals(200, result.code)
        // With default config (empty includes), host should match, so DisabledForHost
        // should NOT be logged. The interceptor exits early because connection is null.
        val disabledEntries = logged.filter { it.second is VerificationResult.Success.DisabledForHost }
        assertTrue(
            disabledEntries.isEmpty(),
            "DisabledForHost should NOT be logged when host matches (empty includes = match all)"
        )
    }

    @Test
    fun `proceed is called on the chain`() {
        val interceptor = certificateTransparencyInterceptor()

        val request = buildRequest("https://example.com/")
        val response = buildResponse(request)
        val chain = FakeChain(request, response)

        interceptor.intercept(chain)

        assertNotNull(chain.proceededRequest, "Chain.proceed should have been called")
        assertEquals(request.url, chain.proceededRequest?.url)
    }

    // endregion
}
