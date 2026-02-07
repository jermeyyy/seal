package com.jermey.seal.core.config

import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.policy.AppleCtPolicy
import com.jermey.seal.core.policy.ChromeCtPolicy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days

class CTConfigurationBuilderTest {

    // ── Default values ──────────────────────────────────────────

    @Test
    fun defaultConfigurationHasChromeCtPolicy() {
        val config = ctConfiguration { }
        assertIs<ChromeCtPolicy>(config.policy)
    }

    @Test
    fun defaultConfigurationIsFailOpen() {
        val config = ctConfiguration { }
        assertFalse(config.failOnError)
    }

    @Test
    fun defaultConfigurationHasGoogleLogListUrl() {
        val config = ctConfiguration { }
        assertEquals(DEFAULT_LOG_LIST_URL, config.logListUrl)
    }

    @Test
    fun defaultConfigurationHasNullLogger() {
        val config = ctConfiguration { }
        assertNull(config.logger)
    }

    @Test
    fun defaultConfigurationHasNullLogListCache() {
        val config = ctConfiguration { }
        assertNull(config.logListCache)
    }

    @Test
    fun defaultConfigurationHasNullLogListNetworkDataSource() {
        val config = ctConfiguration { }
        assertNull(config.logListNetworkDataSource)
    }

    @Test
    fun defaultLogListMaxAgeIs70Days() {
        val config = ctConfiguration { }
        assertEquals(70.days, config.logListMaxAge)
    }

    // ── Host matching via +/- operators ─────────────────────────

    @Test
    fun unaryPlusIncludesHosts() {
        val config = ctConfiguration {
            +"*.example.com"
        }
        assertTrue(config.hostMatcher.matches("api.example.com"))
        assertFalse(config.hostMatcher.matches("other.com"))
    }

    @Test
    fun unaryMinusExcludesHosts() {
        val config = ctConfiguration {
            -"blocked.com"
        }
        assertFalse(config.hostMatcher.matches("blocked.com"))
        assertTrue(config.hostMatcher.matches("other.com"))
    }

    @Test
    fun includeAndExcludeCombination() {
        val config = ctConfiguration {
            +"*.example.com"
            -"internal.example.com"
        }
        assertTrue(config.hostMatcher.matches("api.example.com"))
        assertFalse(config.hostMatcher.matches("internal.example.com"))
    }

    // ── Custom policy ───────────────────────────────────────────

    @Test
    fun customPolicyCanBeSet() {
        val config = ctConfiguration {
            policy = AppleCtPolicy()
        }
        assertIs<AppleCtPolicy>(config.policy)
    }

    // ── failOnError toggle ──────────────────────────────────────

    @Test
    fun failOnErrorCanBeEnabled() {
        val config = ctConfiguration {
            failOnError = true
        }
        assertTrue(config.failOnError)
    }

    // ── Logger callback ─────────────────────────────────────────

    @Test
    fun loggerCallbackIsStoredCorrectly() {
        var captured: String? = null
        val config = ctConfiguration {
            logger = { host, _ -> captured = host }
        }
        assertNotNull(config.logger)
        config.logger!!.invoke("test.com", VerificationResult.Success.InsecureConnection)
        assertEquals("test.com", captured)
    }

    // ── logListUrl ──────────────────────────────────────────────

    @Test
    fun logListUrlCanBeChanged() {
        val customUrl = "https://custom.example.com/log_list.json"
        val config = ctConfiguration {
            logListUrl = customUrl
        }
        assertEquals(customUrl, config.logListUrl)
    }

    // ── logListMaxAge ───────────────────────────────────────────

    @Test
    fun logListMaxAgeCanBeChanged() {
        val config = ctConfiguration {
            logListMaxAge = 30.days
        }
        assertEquals(30.days, config.logListMaxAge)
    }

    // ── DSL function ────────────────────────────────────────────

    @Test
    fun ctConfigurationDslBuildsCorrectConfig() {
        val config = ctConfiguration {
            +"*.example.com"
            -"internal.example.com"
            policy = AppleCtPolicy()
            failOnError = true
            logListUrl = "https://custom.example.com/logs.json"
            logListMaxAge = 14.days
        }

        assertTrue(config.hostMatcher.matches("api.example.com"))
        assertFalse(config.hostMatcher.matches("internal.example.com"))
        assertIs<AppleCtPolicy>(config.policy)
        assertTrue(config.failOnError)
        assertEquals("https://custom.example.com/logs.json", config.logListUrl)
        assertEquals(14.days, config.logListMaxAge)
    }
}
