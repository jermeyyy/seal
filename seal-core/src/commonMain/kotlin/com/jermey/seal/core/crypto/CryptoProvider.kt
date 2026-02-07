package com.jermey.seal.core.crypto

/**
 * Factory function to create a platform-specific [CryptoVerifier] instance.
 * Implemented via expect/actual for Android (java.security) and iOS (Security framework).
 */
public expect fun createCryptoVerifier(): CryptoVerifier
