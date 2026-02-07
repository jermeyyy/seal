package com.jermey.seal.android.okhttp

import android.util.Log
import org.conscrypt.Conscrypt
import java.net.InetAddress
import java.net.Socket
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

/**
 * An SSLSocketFactory wrapper that enables Certificate Transparency on Conscrypt sockets.
 *
 * Must wrap a Conscrypt-backed SSLSocketFactory (created from an SSLContext with Conscrypt provider).
 * Enables SCT (Signed Certificate Timestamp) delivery via the TLS extension by reflectively
 * configuring Conscrypt's internal SSL parameters.
 */
internal class ConscryptCtSocketFactory(
    private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {

    override fun getDefaultCipherSuites(): Array<String> = delegate.defaultCipherSuites

    override fun getSupportedCipherSuites(): Array<String> = delegate.supportedCipherSuites

    override fun createSocket(socket: Socket?, host: String?, port: Int, autoClose: Boolean): Socket {
        val sslSocket = delegate.createSocket(socket, host, port, autoClose)
        enableCt(sslSocket)
        return sslSocket
    }

    override fun createSocket(host: String?, port: Int): Socket {
        val sslSocket = delegate.createSocket(host, port)
        enableCt(sslSocket)
        return sslSocket
    }

    override fun createSocket(host: String?, port: Int, localHost: InetAddress?, localPort: Int): Socket {
        val sslSocket = delegate.createSocket(host, port, localHost, localPort)
        enableCt(sslSocket)
        return sslSocket
    }

    override fun createSocket(host: InetAddress?, port: Int): Socket {
        val sslSocket = delegate.createSocket(host, port)
        enableCt(sslSocket)
        return sslSocket
    }

    override fun createSocket(address: InetAddress?, port: Int, localAddress: InetAddress?, localPort: Int): Socket {
        val sslSocket = delegate.createSocket(address, port, localAddress, localPort)
        enableCt(sslSocket)
        return sslSocket
    }

    /**
     * Enable Certificate Transparency (SCT extension request) on a Conscrypt socket.
     *
     * Conscrypt requires explicit opt-in for the client to request SCTs via the TLS
     * `signed_certificate_timestamp` extension. This is done by reflectively accessing
     * the internal `SSLParametersImpl.setSignedCertTimestamps(true)`.
     */
    private fun enableCt(socket: Socket) {
        if (socket !is SSLSocket || !Conscrypt.isConscrypt(socket)) return
        try {
            // Access the internal sslParameters field on the Conscrypt socket implementation
            val sslParamsField = findField(socket.javaClass, "sslParameters")
            if (sslParamsField != null) {
                sslParamsField.isAccessible = true
                val sslParams = sslParamsField.get(socket)
                if (sslParams != null) {
                    val setMethod = sslParams.javaClass.getDeclaredMethod(
                        "setSignedCertTimestamps",
                        Boolean::class.javaPrimitiveType,
                    )
                    setMethod.isAccessible = true
                    setMethod.invoke(sslParams, true)
                    Log.d("SealCT", "Enabled SCT extension request on Conscrypt socket")
                    return
                }
            }
            Log.w("SealCT", "Could not find sslParameters field on ${socket.javaClass.name}")
        } catch (e: Exception) {
            Log.w("SealCT", "Failed to enable CT on Conscrypt socket: ${e.message}")
        }
    }

    /** Walk the class hierarchy to find a declared field by name. */
    private fun findField(clazz: Class<*>, name: String): java.lang.reflect.Field? {
        var current: Class<*>? = clazz
        while (current != null) {
            try {
                return current.getDeclaredField(name)
            } catch (_: NoSuchFieldException) {
                current = current.superclass
            }
        }
        return null
    }
}
