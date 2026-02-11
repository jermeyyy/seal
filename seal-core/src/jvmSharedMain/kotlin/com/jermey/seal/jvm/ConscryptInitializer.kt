package com.jermey.seal.jvm

import org.conscrypt.Conscrypt
import java.security.Security
import java.util.logging.Logger
import javax.net.ssl.SSLSocket

public object ConscryptInitializer {
    private val logger = Logger.getLogger("SealCT")
    private var initialized = false
    private var available = false

    public val isAvailable: Boolean get() = available

    public fun initialize() {
        if (!initialized) {
            initialized = true
            try {
                Security.insertProviderAt(Conscrypt.newProvider(), 1)
                available = true
            } catch (t: Throwable) {
                logger.warning(
                    "Conscrypt native library unavailable (${t.message}). " +
                        "TLS extension SCTs and OCSP SCTs will not be available."
                )
            }
        }
    }

    public fun isConscryptSocket(socket: SSLSocket): Boolean {
        if (!available) return false
        return Conscrypt.isConscrypt(socket)
    }
}
