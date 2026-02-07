package com.jermey.seal.core.config

import com.jermey.seal.core.host.HostMatcher
import com.jermey.seal.core.loglist.LogListCache
import com.jermey.seal.core.loglist.LogListDataSource
import com.jermey.seal.core.model.VerificationResult
import com.jermey.seal.core.policy.CTPolicy
import kotlin.time.Duration

/**
 * Immutable configuration for CT verification.
 * Created via the [CTConfigurationBuilder] DSL.
 */
public class CTConfiguration internal constructor(
    /** Host matcher for include/exclude rules. */
    public val hostMatcher: HostMatcher,
    /** CT compliance policy to evaluate. */
    public val policy: CTPolicy,
    /** Whether to fail-closed (true) or fail-open (false) on verification failures. */
    public val failOnError: Boolean,
    /** Optional logger callback for verification results. */
    public val logger: ((host: String, result: VerificationResult) -> Unit)?,
    /** URL of the CT log list JSON. */
    public val logListUrl: String,
    /** Optional custom log list cache. */
    public val logListCache: LogListCache?,
    /** Optional custom network data source for the log list. */
    public val logListNetworkDataSource: LogListDataSource?,
    /** Maximum age of the cached log list before refresh. */
    public val logListMaxAge: Duration,
)
