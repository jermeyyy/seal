@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("UNCHECKED_CAST")

package com.jermey.seal.core.crypto

import com.jermey.seal.core.model.SignatureAlgorithm
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDataCreate
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.create
import platform.Security.SecKeyCreateWithData
import platform.Security.SecKeyVerifySignature
import platform.Security.kSecAttrKeyClass
import platform.Security.kSecAttrKeyClassPublic
import platform.Security.kSecAttrKeyType
import platform.Security.kSecAttrKeyTypeEC
import platform.Security.kSecAttrKeyTypeRSA
import platform.Security.kSecKeyAlgorithmECDSASignatureMessageX962SHA256
import platform.Security.kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256
import platform.posix.uint8_tVar

/**
 * iOS actual implementation of [createCryptoVerifier].
 */
public actual fun createCryptoVerifier(): CryptoVerifier = IosCryptoVerifier()

/**
 * iOS [CryptoVerifier] using Apple Security and CommonCrypto frameworks.
 */
internal class IosCryptoVerifier : CryptoVerifier {

    override fun verifySignature(
        publicKeyBytes: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm,
    ): Boolean = try {
        val (keyType, secAlgorithm) = when (algorithm) {
            SignatureAlgorithm.ECDSA -> kSecAttrKeyTypeEC to kSecKeyAlgorithmECDSASignatureMessageX962SHA256
            SignatureAlgorithm.RSA -> kSecAttrKeyTypeRSA to kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256
            else -> return false
        }

        val keyData = publicKeyBytes.toNSData()

        val attributes = CFDictionaryCreateMutable(kCFAllocatorDefault, 2, null, null)
        try {
            CFDictionarySetValue(attributes, kSecAttrKeyType, keyType)
            CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

            val secKey = SecKeyCreateWithData(
                CFBridgingRetain(keyData) as platform.CoreFoundation.CFDataRef,
                attributes,
                null,
            ) ?: return false

            try {
                val dataRef = CFBridgingRetain(data.toNSData()) as platform.CoreFoundation.CFDataRef
                val signatureRef = CFBridgingRetain(signature.toNSData()) as platform.CoreFoundation.CFDataRef

                try {
                    SecKeyVerifySignature(secKey, secAlgorithm, dataRef, signatureRef, null)
                } finally {
                    CFRelease(dataRef)
                    CFRelease(signatureRef)
                }
            } finally {
                CFRelease(secKey)
            }
        } finally {
            CFRelease(attributes)
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        false
    }

    override fun sha256(data: ByteArray): ByteArray = memScoped {
        val output = allocArray<uint8_tVar>(CC_SHA256_DIGEST_LENGTH)
        data.usePinned { pinned ->
            CC_SHA256(pinned.addressOf(0), data.size.toUInt(), output.reinterpret())
        }
        ByteArray(CC_SHA256_DIGEST_LENGTH).also { result ->
            result.usePinned { pinned ->
                platform.posix.memcpy(pinned.addressOf(0), output, CC_SHA256_DIGEST_LENGTH.toULong())
            }
        }
    }

    private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
        @Suppress("CAST_NEVER_SUCCEEDS")
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}
