package com.jermey.seal.android

import org.conscrypt.Conscrypt
import java.security.Security
import javax.net.ssl.SSLSocket

/**
 * Helper to ensure Conscrypt is installed as the default security provider.
 * Must be called early in `Application.onCreate()`.
 */
public object ConscryptInitializer {
    private var initialized = false

    /**
     * Install Conscrypt as the highest-priority security provider.
     * Safe to call multiple times — only the first call has effect.
     */
    public fun initialize() {
        if (!initialized) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            initialized = true
        }
    }

    /**
     * Returns `true` if the given [socket] is a Conscrypt-managed socket.
     */
    public fun isConscryptSocket(socket: SSLSocket): Boolean =
        Conscrypt.isConscrypt(socket)
}
