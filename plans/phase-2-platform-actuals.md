# Phase 2: seal-core — Platform Actual Implementations

> **Prerequisites**: Phase 1A and 1B (specifically Tasks 1.8, 1.12, 1.13)
> **Summary**: Platform-specific implementations of `expect` declarations from Phase 1. This includes CryptoVerifier actuals for Android (JVM) and iOS (Security.framework), disk cache implementations for both platforms, and platform-specific resource loading for the embedded log list.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 1A and 1B (specifically Tasks 1.8, 1.12, 1.13).

---

## Dependency Graph (Phase 2)

```
2.1  Android CryptoVerifier       ← 1.8
2.2  iOS CryptoVerifier           ← 1.8
2.3  Android disk cache           ← 1.12
2.4  iOS disk cache               ← 1.12
2.5  Resource loading             ← 1.13
```

**Parallelization**: 
- Tasks 2.1 + 2.2 can be done in parallel (both depend on 1.8, platform-independent).
- Tasks 2.3 + 2.4 + 2.5 can be done in parallel (after 1.12 / 1.13).

---

## Tasks

---

### Task 2.1: Android CryptoVerifier Implementation

**Description**: Implement `CryptoVerifier` actual for Android/JVM using `java.security.Signature` and `java.security.MessageDigest`.

**Files to create**:
- `seal-core/src/androidMain/kotlin/com/jermey/seal/core/crypto/CryptoVerifier.android.kt`

**Implementation**:
```kotlin
actual fun createCryptoVerifier(): CryptoVerifier = JvmCryptoVerifier()

internal class JvmCryptoVerifier : CryptoVerifier {
    override fun verifySignature(
        publicKeyBytes: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm,
    ): Boolean {
        val keySpec = X509EncodedKeySpec(publicKeyBytes)
        val keyFactory = KeyFactory.getInstance(algorithm.jvmAlgorithm)
        val publicKey = keyFactory.generatePublic(keySpec)
        val sig = Signature.getInstance(algorithm.jvmSignatureName)
        sig.initVerify(publicKey)
        sig.update(data)
        return sig.verify(signature)
    }
    
    override fun sha256(data: ByteArray): ByteArray =
        MessageDigest.getInstance("SHA-256").digest(data)
}
```

**Dependencies**: 1.8
**Acceptance Criteria**: Verifies ECDSA and RSA signatures; SHA-256 produces correct hashes
**Complexity**: Low

---

### Task 2.2: iOS CryptoVerifier Implementation

**Description**: Implement `CryptoVerifier` actual for iOS using Apple's `Security.framework` via Kotlin/Native interop. Use `SecKeyVerifySignature` for signatures and `CC_SHA256` (CommonCrypto) for hashing.

**Files to create**:
- `seal-core/src/iosMain/kotlin/com/jermey/seal/core/crypto/CryptoVerifier.ios.kt`

**Implementation notes**:
- Use `SecKeyCreateWithData` to import raw public key bytes
- Use `SecKeyVerifySignature` with `kSecKeyAlgorithmECDSASignatureMessageX962SHA256` for ECDSA
- Use `kSecKeyAlgorithmRSASignatureMessagePKCS1v15SHA256` for RSA
- Use `CC_SHA256` from `platform.CommonCrypto` for hashing
- Handle `CFData` / `NSData` bridging

**Dependencies**: 1.8
**Acceptance Criteria**: Verifies ECDSA and RSA signatures on iOS; SHA-256 matches JVM output for same input; handles memory management correctly (autoreleasepool)
**Complexity**: High

---

### Task 2.3: Platform-Specific Disk Cache (Android)

**Description**: Implement a disk cache for the log list on Android using the app's cache directory.

**Files to create**:
- `seal-core/src/androidMain/kotlin/com/jermey/seal/core/loglist/DiskLogListCache.android.kt`

**Implementation**: Simple file-based cache using `context.cacheDir`. Store JSON + metadata (timestamp, ETag).

**Dependencies**: 1.12
**Acceptance Criteria**: Caches log list to disk; survives app restart; respects staleness
**Complexity**: Low

---

### Task 2.4: Platform-Specific Disk Cache (iOS)

**Description**: Implement a disk cache for the log list on iOS using `NSCachesDirectory`.

**Files to create**:
- `seal-core/src/iosMain/kotlin/com/jermey/seal/core/loglist/DiskLogListCache.ios.kt`

**Dependencies**: 1.12
**Acceptance Criteria**: Caches log list to iOS file system; survives app restart
**Complexity**: Low

---

### Task 2.5: Platform-Specific Resource Loading

**Description**: Implement `expect`/`actual` for loading the embedded baseline log list from platform resources.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.kt` (expect)
- `seal-core/src/androidMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.android.kt`
- `seal-core/src/iosMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.ios.kt`

**Android**: Load from Android resources or assets
**iOS**: Load from the framework bundle

**Dependencies**: 1.13
**Acceptance Criteria**: Resource loads correctly on both platforms; returns byte array of the embedded JSON
**Complexity**: Medium
