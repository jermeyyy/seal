package com.jermey.seal.core.model

/**
 * Lifecycle state of a Certificate Transparency log server as published in the
 * Google CT Log List V3.
 *
 * The state determines whether SCTs from a log are accepted during verification.
 *
 * @see LogServer
 */
public enum class LogState {
    /** Log has been submitted but is not yet approved for inclusion. */
    PENDING,

    /** Log has passed qualification testing but is not yet usable. */
    QUALIFIED,

    /** Log is active and accepting submissions — SCTs from this log are trusted. */
    USABLE,

    /** Log is no longer accepting new submissions but existing SCTs remain valid. */
    READ_ONLY,

    /** Log has been permanently decommissioned — SCTs may still be accepted depending on policy. */
    RETIRED,

    /** Log has been rejected and SCTs from it should never be trusted. */
    REJECTED,
}
