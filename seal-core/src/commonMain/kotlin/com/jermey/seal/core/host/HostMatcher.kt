package com.jermey.seal.core.host

/**
 * A builder/container that holds include and exclude [HostPattern]s.
 *
 * Excludes always take precedence over includes.
 * If no include patterns are specified, all hosts match (unless explicitly excluded).
 *
 * Usage with DSL operators:
 * ```kotlin
 * val matcher = HostMatcher().apply {
 *     +"*.example.com"   // include
 *     -"internal.example.com" // exclude
 * }
 * matcher.matches("api.example.com")      // true
 * matcher.matches("internal.example.com") // false
 * ```
 */
public class HostMatcher {

    private val includes: MutableList<HostPattern> = mutableListOf()
    private val excludes: MutableList<HostPattern> = mutableListOf()

    /** Add a host pattern to the include list. */
    public operator fun String.unaryPlus() {
        includes.add(HostPattern(this))
    }

    /** Add a host pattern to the exclude list. */
    public operator fun String.unaryMinus() {
        excludes.add(HostPattern(this))
    }

    /**
     * Returns `true` if the given [hostname] is included and not excluded.
     *
     * - If [hostname] matches any exclude pattern, returns `false`.
     * - If no include patterns are configured, all non-excluded hosts match.
     * - Otherwise [hostname] must match at least one include pattern.
     */
    public fun matches(hostname: String): Boolean {
        // Excludes always take precedence
        if (excludes.any { it.matches(hostname) }) return false
        // If no includes, everything matches
        if (includes.isEmpty()) return true
        // Must match at least one include
        return includes.any { it.matches(hostname) }
    }
}
