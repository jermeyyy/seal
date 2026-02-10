package com.jermey.seal.core.config

import com.jermey.seal.core.host.HostMatcher
import com.jermey.seal.core.loglist.LogListCache
import com.jermey.seal.core.loglist.LogListDataSource
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.policy.CTPolicy
import com.jermey.seal.core.policy.AppleCtPolicy
import com.jermey.seal.core.policy.ChromeCtPolicy
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Default URL for the Google CT log list V3.
 */
public const val DEFAULT_LOG_LIST_URL: String =
    "https://www.gstatic.com/ct/log_list/v3/log_list.json"

/**
 * DSL builder for [CTConfiguration].
 *
 * Usage:
 * ```kotlin
 * val config = ctConfiguration {
 *     +"*.example.com"       // include host
 *     -"internal.example.com" // exclude host
 *     policy = ChromeCtPolicy()
 *     failOnError = false    // fail-open (default)
 *     logger = { host, result -> println("CT: $host -> $result") }
 * }
 * ```
 */
public class CTConfigurationBuilder {
    private val hostMatcher: HostMatcher = HostMatcher()

    /** Include a host pattern for CT verification. */
    public operator fun String.unaryPlus() {
        with(hostMatcher) { this@unaryPlus.unaryPlus() }
    }

    /** Exclude a host pattern from CT verification. */
    public operator fun String.unaryMinus() {
        with(hostMatcher) { this@unaryMinus.unaryMinus() }
    }

    /**
     * CT compliance policy. Defaults to [ChromeCtPolicy].
     *
     * Available policies:
     * - [ChromeCtPolicy] — Requires Google + non-Google operator diversity. Strictest option.
     * - [AppleCtPolicy] — Requires 2+ distinct operators (any). More lenient.
     *
     * Example: `policy = AppleCtPolicy()`
     */
    public var policy: CTPolicy = ChromeCtPolicy()

    /** Use Chrome's CT policy (default). Requires Google + non-Google operator diversity. */
    public fun useChromePolicy() {
        policy = ChromeCtPolicy()
    }

    /** Use Apple's CT policy. Requires 2+ distinct operators (any). More lenient than Chrome. */
    public fun useApplePolicy() {
        policy = AppleCtPolicy()
    }

    /** Whether to throw/fail on CT verification failure. Defaults to `false` (fail-open). */
    public var failOnError: Boolean = false

    /** Optional callback to log verification results. */
    public var logger: ((host: String, result: VerificationResult) -> Unit)? = null

    /** URL of the CT log list JSON. Defaults to Google's V3 log list. */
    public var logListUrl: String = DEFAULT_LOG_LIST_URL

    /** Optional custom cache for the log list. If null, an in-memory cache is used. */
    public var logListCache: LogListCache? = null

    /** Optional custom network data source for fetching the log list. */
    public var logListNetworkDataSource: LogListDataSource? = null

    /** Maximum age of the cached log list before refresh. Defaults to 70 days. */
    public var logListMaxAge: Duration = 70.days

    /**
     * Build the immutable [CTConfiguration].
     */
    public fun build(): CTConfiguration = CTConfiguration(
        hostMatcher = hostMatcher,
        policy = policy,
        failOnError = failOnError,
        logger = logger,
        logListUrl = logListUrl,
        logListCache = logListCache,
        logListNetworkDataSource = logListNetworkDataSource,
        logListMaxAge = logListMaxAge,
    )
}

/**
 * Create a [CTConfiguration] using the DSL builder.
 */
public fun ctConfiguration(block: CTConfigurationBuilder.() -> Unit): CTConfiguration {
    return CTConfigurationBuilder().apply(block).build()
}
