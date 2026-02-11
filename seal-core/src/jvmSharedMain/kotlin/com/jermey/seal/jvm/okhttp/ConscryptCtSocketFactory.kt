package com.jermey.seal.jvm.okhttp

import com.jermey.seal.jvm.ConscryptInitializer
import org.conscrypt.Conscrypt
import java.net.InetAddress
import java.net.Socket
import java.util.logging.Logger
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

internal class ConscryptCtSocketFactory(
    private val delegate: SSLSocketFactory,
) : SSLSocketFactory() {

    private val logger = Logger.getLogger("SealCT")

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

    private fun enableCt(socket: Socket) {
        if (!ConscryptInitializer.isAvailable) return
        if (socket !is SSLSocket || !Conscrypt.isConscrypt(socket)) return
        try {
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
                    logger.fine("Enabled SCT extension request on Conscrypt socket")
                    return
                }
            }
            logger.warning("Could not find sslParameters field on ${socket.javaClass.name}")
        } catch (e: Exception) {
            logger.warning("Failed to enable CT on Conscrypt socket: ${e.message}")
        }
    }

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
