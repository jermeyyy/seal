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

internal class SecTrustCertificateExtractor {

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
