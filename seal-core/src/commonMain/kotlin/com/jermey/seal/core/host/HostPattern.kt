package com.jermey.seal.core.host

/**
 * Represents a single host pattern for matching hostnames.
 *
 * Supported patterns:
 * - `"example.com"` — exact match
 * - `"*.example.com"` — matches exactly one subdomain level (e.g., `api.example.com` but NOT `a.b.example.com`)
 * - `"**.example.com"` — matches one or more subdomain levels (e.g., `api.example.com`, `a.b.example.com`)
 * - `"**"` — matches everything
 *
 * All matching is case-insensitive.
 */
public class HostPattern(public val pattern: String) {

    private val normalizedPattern: String = pattern.lowercase()

    /**
     * Returns `true` if the given [hostname] matches this pattern.
     */
    public fun matches(hostname: String): Boolean {
        val normalizedHost = hostname.lowercase()

        return when {
            // "**" matches everything
            normalizedPattern == "**" -> true

            // "**.suffix" — matches one or more subdomain labels before suffix
            normalizedPattern.startsWith("**.") -> {
                val suffix = normalizedPattern.removePrefix("**.")
                normalizedHost.endsWith(".$suffix") && normalizedHost.length > suffix.length + 1
            }

            // "*.suffix" — matches exactly one subdomain label before suffix
            normalizedPattern.startsWith("*.") -> {
                val suffix = normalizedPattern.removePrefix("*.")
                if (!normalizedHost.endsWith(".$suffix")) return false
                val prefix = normalizedHost.removeSuffix(".$suffix")
                // Must be a single label (no dots)
                prefix.isNotEmpty() && '.' !in prefix
            }

            // Exact match
            else -> normalizedHost == normalizedPattern
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HostPattern) return false
        return normalizedPattern == other.normalizedPattern
    }

    override fun hashCode(): Int = normalizedPattern.hashCode()

    override fun toString(): String = "HostPattern($pattern)"
}
