@file:OptIn(ExperimentalForeignApi::class)

package com.jermey.seal.ios.sectrust

import kotlinx.cinterop.ExperimentalForeignApi
import platform.CoreFoundation.CFBooleanGetValue
import platform.CoreFoundation.CFDictionaryGetValue
import platform.CoreFoundation.CFDictionaryRef
import platform.CoreFoundation.CFStringCreateWithCString
import platform.CoreFoundation.CFStringRef
import platform.CoreFoundation.kCFAllocatorDefault
import platform.CoreFoundation.kCFStringEncodingUTF8
import platform.CoreFoundation.kCFBooleanTrue
import platform.CoreFoundation.CFRelease
import platform.Security.SecTrustCopyResult
import platform.Security.SecTrustRef

/**
 * Checks the OS-level Certificate Transparency verification result
 * from a [SecTrust] evaluation.
 */
internal class SecTrustCtChecker {

    /**
     * Check if the OS reports CT compliance for the given [SecTrust] evaluation.
     *
     * Uses [SecTrustCopyResult] to obtain the trust evaluation dictionary,
     * then reads the `TrustCertificateTransparency` key.
     *
     * @param secTrust the evaluated [SecTrustRef] to check
     * @return `true` if the OS reports CT compliance, `false` otherwise
     */
    fun checkCtCompliance(secTrust: SecTrustRef): Boolean = try {
        val resultDict: CFDictionaryRef = SecTrustCopyResult(secTrust)
            ?: return false

        try {
            val ctKey: CFStringRef = CFStringCreateWithCString(
                kCFAllocatorDefault,
                "TrustCertificateTransparency",
                kCFStringEncodingUTF8,
            ) ?: return false

            try {
                val value = CFDictionaryGetValue(resultDict, ctKey)
                value == kCFBooleanTrue
            } finally {
                CFRelease(ctKey)
            }
        } finally {
            CFRelease(resultDict)
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        false
    }
}
