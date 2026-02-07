@file:OptIn(ExperimentalForeignApi::class)

package com.jermey.seal.ios.sectrust

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreFoundation.CFArrayGetCount
import platform.CoreFoundation.CFArrayGetValueAtIndex
import platform.CoreFoundation.CFArrayRef
import platform.CoreFoundation.CFDataGetBytePtr
import platform.CoreFoundation.CFDataGetLength
import platform.CoreFoundation.CFDataRef
import platform.CoreFoundation.CFRelease
import platform.Security.SecCertificateCopyData
import platform.Security.SecTrustCopyCertificateChain
import platform.Security.SecTrustRef
import platform.posix.memcpy

/**
 * Extracts DER-encoded certificates from a [SecTrust] object.
 */
internal class SecTrustCertificateExtractor {

    /**
     * Extract DER-encoded certificates from a [SecTrust] object.
     *
     * Uses [SecTrustCopyCertificateChain] (iOS 15+) to obtain the certificate
     * chain and [SecCertificateCopyData] to get DER bytes for each certificate.
     *
     * @param secTrust the evaluated [SecTrustRef] to extract certificates from
     * @return a list of [ByteArray] where index 0 is the leaf certificate,
     *         or an empty list on failure
     */
    fun extractCertificates(secTrust: SecTrustRef): List<ByteArray> = try {
        val chain: CFArrayRef = SecTrustCopyCertificateChain(secTrust)
            ?: return emptyList()

        try {
            val count = CFArrayGetCount(chain)
            val certificates = mutableListOf<ByteArray>()

            for (i in 0 until count) {
                val certPtr = CFArrayGetValueAtIndex(chain, i) ?: continue

                @Suppress("UNCHECKED_CAST")
                val certData: CFDataRef = SecCertificateCopyData(certPtr as platform.Security.SecCertificateRef)
                    ?: continue

                try {
                    val length = CFDataGetLength(certData).toInt()
                    val bytePtr = CFDataGetBytePtr(certData)

                    if (bytePtr != null && length > 0) {
                        val bytes = ByteArray(length)
                        bytes.usePinned { pinned ->
                            memcpy(pinned.addressOf(0), bytePtr, length.toULong())
                        }
                        certificates.add(bytes)
                    }
                } finally {
                    CFRelease(certData)
                }
            }

            certificates
        } finally {
            CFRelease(chain)
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        emptyList()
    }
}
