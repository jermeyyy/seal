@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
@file:Suppress("UNCHECKED_CAST")

package com.jermey.seal.core.crypto

import com.jermey.seal.core.asn1.Asn1Parser
import com.jermey.seal.core.model.SignatureAlgorithm
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.usePinned
import kotlinx.cinterop.value
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreFoundation.CFDictionaryCreateMutable
import platform.CoreFoundation.CFDictionarySetValue
import platform.CoreFoundation.CFErrorRefVar
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.kCFAllocatorDefault
import platform.Foundation.CFBridgingRelease
import platform.Foundation.CFBridgingRetain
import platform.Foundation.NSData
import platform.Foundation.NSLog
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

        // Strip SPKI header: SecKeyCreateWithData expects raw key format
        // (X9.63 for EC, PKCS#1 for RSA), not the full SPKI wrapper.
        val rawKeyBytes = stripSpkiHeader(publicKeyBytes)
        val keyData = rawKeyBytes.toNSData()

        val attributes = CFDictionaryCreateMutable(kCFAllocatorDefault, 2, null, null)
        try {
            CFDictionarySetValue(attributes, kSecAttrKeyType, keyType)
            CFDictionarySetValue(attributes, kSecAttrKeyClass, kSecAttrKeyClassPublic)

            val keyDataRef = CFBridgingRetain(keyData) as platform.CoreFoundation.CFDataRef
            try {
                val secKey = memScoped {
                    val errorRef = alloc<CFErrorRefVar>()
                    val key = SecKeyCreateWithData(keyDataRef, attributes, errorRef.ptr)
                    if (key == null) {
                        val error = errorRef.value
                        if (error != null) {
                            val nsError = CFBridgingRelease(error)
                            NSLog("Seal: SecKeyCreateWithData failed: %@", nsError.toString())
                        }
                        return false
                    }
                    key
                }

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
                CFRelease(keyDataRef)
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

    /**
     * Strips the SPKI (SubjectPublicKeyInfo) DER wrapper from a public key,
     * returning the raw key bytes that Apple's Security framework expects.
     *
     * For EC keys: returns the 65-byte X9.63 uncompressed point (04 || x || y).
     * For RSA keys: returns the PKCS#1 RSAPublicKey DER encoding.
     */
    private fun stripSpkiHeader(publicKeyBytes: ByteArray): ByteArray = try {
        val spki = Asn1Parser.parse(publicKeyBytes)
        // SPKI = SEQUENCE { AlgorithmIdentifier, BIT STRING }
        val bitString = spki.children.getOrNull(1)
        if (bitString != null && bitString.rawValue.isNotEmpty()) {
            // BIT STRING rawValue starts with the unused-bits byte (0x00), skip it.
            bitString.rawValue.copyOfRange(1, bitString.rawValue.size)
        } else {
            // Fallback: return original bytes if parsing fails.
            publicKeyBytes
        }
    } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
        // If ASN.1 parsing fails, return original bytes and let SecKey handle the error.
        publicKeyBytes
    }

    private fun ByteArray.toNSData(): NSData = this.usePinned { pinned ->
        @Suppress("CAST_NEVER_SUCCEEDS")
        NSData.create(bytes = pinned.addressOf(0), length = this.size.toULong())
    }
}
