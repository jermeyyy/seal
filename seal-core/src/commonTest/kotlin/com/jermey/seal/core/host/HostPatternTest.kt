package com.jermey.seal.core.host

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HostPatternTest {

    // ── Exact match ──────────────────────────────────────────────

    @Test
    fun exactMatch() {
        val pattern = HostPattern("example.com")
        assertTrue(pattern.matches("example.com"))
    }

    @Test
    fun exactMatchIsCaseInsensitive() {
        val pattern = HostPattern("Example.COM")
        assertTrue(pattern.matches("EXAMPLE.com"))
        assertTrue(pattern.matches("example.com"))
    }

    @Test
    fun exactMatchRejectsSubdomain() {
        val pattern = HostPattern("example.com")
        assertFalse(pattern.matches("www.example.com"))
    }

    @Test
    fun exactMatchRejectsDifferentHost() {
        val pattern = HostPattern("example.com")
        assertFalse(pattern.matches("example.org"))
    }

    // ── Single wildcard (*) ──────────────────────────────────────

    @Test
    fun singleWildcardMatchesOneLabel() {
        val pattern = HostPattern("*.example.com")
        assertTrue(pattern.matches("api.example.com"))
        assertTrue(pattern.matches("www.example.com"))
    }

    @Test
    fun singleWildcardRejectsExactBase() {
        val pattern = HostPattern("*.example.com")
        assertFalse(pattern.matches("example.com"))
    }

    @Test
    fun singleWildcardRejectsMultipleLabels() {
        val pattern = HostPattern("*.example.com")
        assertFalse(pattern.matches("a.b.example.com"))
    }

    @Test
    fun singleWildcardIsCaseInsensitive() {
        val pattern = HostPattern("*.Example.COM")
        assertTrue(pattern.matches("API.example.com"))
    }

    // ── Double wildcard (**) ─────────────────────────────────────

    @Test
    fun doubleWildcardMatchesSingleLabel() {
        val pattern = HostPattern("**.example.com")
        assertTrue(pattern.matches("api.example.com"))
    }

    @Test
    fun doubleWildcardMatchesMultipleLabels() {
        val pattern = HostPattern("**.example.com")
        assertTrue(pattern.matches("a.b.example.com"))
        assertTrue(pattern.matches("a.b.c.d.example.com"))
    }

    @Test
    fun doubleWildcardRejectsExactBase() {
        val pattern = HostPattern("**.example.com")
        assertFalse(pattern.matches("example.com"))
    }

    @Test
    fun doubleWildcardIsCaseInsensitive() {
        val pattern = HostPattern("**.Example.COM")
        assertTrue(pattern.matches("deep.sub.EXAMPLE.com"))
    }

    // ── Match-all (**) ───────────────────────────────────────────

    @Test
    fun matchAllMatchesEverything() {
        val pattern = HostPattern("**")
        assertTrue(pattern.matches("example.com"))
        assertTrue(pattern.matches("a.b.c.example.com"))
        assertTrue(pattern.matches("localhost"))
    }

    // ── Edge cases ───────────────────────────────────────────────

    @Test
    fun emptyHostnameDoesNotMatchExact() {
        val pattern = HostPattern("example.com")
        assertFalse(pattern.matches(""))
    }

    @Test
    fun emptyHostnameMatchesMatchAll() {
        val pattern = HostPattern("**")
        assertTrue(pattern.matches(""))
    }

    @Test
    fun dotOnlyHostname() {
        val pattern = HostPattern("*.com")
        assertFalse(pattern.matches(".com"))
    }

    @Test
    fun emptyPattern() {
        val pattern = HostPattern("")
        assertFalse(pattern.matches("example.com"))
        assertTrue(pattern.matches(""))
    }
}
