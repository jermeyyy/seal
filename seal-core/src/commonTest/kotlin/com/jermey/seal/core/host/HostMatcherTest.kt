package com.jermey.seal.core.host

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostMatcherTest {

    // ── No includes = match everything ───────────────────────────

    @Test
    fun noIncludesMatchesEverything() {
        val matcher = HostMatcher()
        assertTrue(matcher.matches("example.com"))
        assertTrue(matcher.matches("anything.org"))
    }

    // ── Include patterns ─────────────────────────────────────────

    @Test
    fun includeExactHost() {
        val matcher = HostMatcher().apply {
            +"example.com"
        }
        assertTrue(matcher.matches("example.com"))
        assertFalse(matcher.matches("other.com"))
    }

    @Test
    fun includeWildcardPattern() {
        val matcher = HostMatcher().apply {
            +"*.example.com"
        }
        assertTrue(matcher.matches("api.example.com"))
        assertFalse(matcher.matches("example.com"))
        assertFalse(matcher.matches("other.com"))
    }

    @Test
    fun multipleIncludes() {
        val matcher = HostMatcher().apply {
            +"*.example.com"
            +"*.example.org"
        }
        assertTrue(matcher.matches("api.example.com"))
        assertTrue(matcher.matches("www.example.org"))
        assertFalse(matcher.matches("other.net"))
    }

    // ── Exclude patterns ─────────────────────────────────────────

    @Test
    fun excludeTakesPrecedenceOverAllIncludes() {
        val matcher = HostMatcher().apply {
            +"*.example.com"
            -"secret.example.com"
        }
        assertTrue(matcher.matches("api.example.com"))
        assertFalse(matcher.matches("secret.example.com"))
    }

    @Test
    fun excludeWithNoIncludes() {
        val matcher = HostMatcher().apply {
            -"blocked.com"
        }
        assertTrue(matcher.matches("example.com"))
        assertFalse(matcher.matches("blocked.com"))
    }

    @Test
    fun excludeWildcard() {
        val matcher = HostMatcher().apply {
            +"**"
            -"*.internal.example.com"
        }
        assertTrue(matcher.matches("api.example.com"))
        assertFalse(matcher.matches("secret.internal.example.com"))
    }

    // ── DSL operator tests ───────────────────────────────────────

    @Test
    fun unaryPlusAddsInclude() {
        val matcher = HostMatcher().apply {
            +"example.com"
        }
        assertTrue(matcher.matches("example.com"))
        assertFalse(matcher.matches("other.com"))
    }

    @Test
    fun unaryMinusAddsExclude() {
        val matcher = HostMatcher().apply {
            -"example.com"
        }
        assertFalse(matcher.matches("example.com"))
        assertTrue(matcher.matches("other.com"))
    }

    // ── Combined scenarios ───────────────────────────────────────

    @Test
    fun complexScenario() {
        val matcher = HostMatcher().apply {
            +"**.example.com"
            +"*.example.org"
            -"internal.example.com"
            -"**.secret.example.com"
        }
        // Included
        assertTrue(matcher.matches("api.example.com"))
        assertTrue(matcher.matches("a.b.example.com"))
        assertTrue(matcher.matches("www.example.org"))

        // Excluded
        assertFalse(matcher.matches("internal.example.com"))
        assertFalse(matcher.matches("deep.secret.example.com"))

        // Not included
        assertFalse(matcher.matches("example.com"))
        assertFalse(matcher.matches("random.net"))
    }

    @Test
    fun caseInsensitiveMatching() {
        val matcher = HostMatcher().apply {
            +"*.Example.COM"
            -"Secret.EXAMPLE.com"
        }
        assertTrue(matcher.matches("API.example.com"))
        assertFalse(matcher.matches("secret.example.com"))
    }
}
