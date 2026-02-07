package com.jermey.seal.core.model

/**
 * Represents a Certificate Transparency log operator (e.g., Google, Cloudflare, DigiCert).
 *
 * An operator manages one or more [LogServer]s. CT policies often require SCTs from
 * logs operated by distinct operators to prevent a single entity from controlling
 * the transparency guarantees.
 *
 * @property name The human-readable name of the operator.
 * @property logs The list of CT log servers managed by this operator.
 * @see LogServer
 */
public data class LogOperator(
    public val name: String,
    public val logs: List<LogServer>,
)
