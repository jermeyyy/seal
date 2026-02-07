# Phase 4: seal-ios — iOS Integration

> **Prerequisites**: Phase 0 (Task 0.6), Phase 1B (Tasks 1.12, 1.15, 1.16)
> **Summary**: iOS-specific integrations including SecTrust CT compliance checking, certificate extraction from trust objects, the iOS-specific hybrid CT verifier (manual embedded SCT verification + OS-level TLS/OCSP), URLSession delegate integration helper, and iOS disk cache.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 0 (Task 0.6), Phase 1B (Tasks 1.12, 1.15, 1.16).

---

## Dependency Graph (Phase 4)

```
4.1  SecTrust CT result checker   ← 0.6
4.2  SecTrust cert extractor      ← 0.6
4.3  iOS CT verifier              ← 1.15, 4.1, 4.2
4.4  URLSession delegate helper   ← 1.16, 4.3
4.5  iOS disk cache               ← 1.12
```

**Parallelization**: Tasks 4.1 + 4.2 can be done in parallel (both depend on 0.6, independent of each other).

---

## Tasks

---

### Task 4.1: SecTrust CT Result Checker

**Description**: Check the OS-level CT verification result from a `SecTrust` evaluation using the `kSecTrustCertificateTransparency` key.

**Files to create**:
- `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/sectrust/SecTrustCtChecker.kt`

**Implementation**:
```kotlin
internal class SecTrustCtChecker {
    fun checkCtCompliance(secTrust: SecTrustRef): Boolean {
        val result = SecTrustCopyResult(secTrust)
        // Extract kSecTrustCertificateTransparency boolean from result dictionary
        return result?.get(kSecTrustCertificateTransparency) as? Boolean ?: false
    }
}
```

**Dependencies**: 0.6
**Acceptance Criteria**: Correctly reads CT compliance flag from SecTrust evaluation
**Complexity**: Medium

---

### Task 4.2: SecTrust Certificate Extractor

**Description**: Extract DER-encoded certificates from a `SecTrust` object for manual embedded SCT verification.

**Files to create**:
- `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/sectrust/SecTrustCertificateExtractor.kt`

**Implementation**:
- Use `SecTrustGetCertificateCount` and `SecTrustGetCertificateAtIndex` (or newer `SecTrustCopyCertificateChain`)
- Convert `SecCertificate` to DER bytes via `SecCertificateCopyData`
- Return list of `ByteArray` (leaf first)

**Dependencies**: 0.6
**Acceptance Criteria**: Extracts DER bytes of all certificates from a trust chain
**Complexity**: Medium

---

### Task 4.3: iOS Certificate Transparency Verifier

**Description**: Implement the iOS-specific CT verification logic that combines manual embedded SCT verification with OS-level TLS/OCSP verification.

**Files to create**:
- `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/IosCertificateTransparencyVerifier.kt`

**Logic**:
1. Extract certificates from `SecTrust` (Task 4.2)
2. Parse embedded SCTs from leaf certificate using `:seal-core` parser
3. Verify embedded SCTs against library's log list
4. Check `kSecTrustCertificateTransparency` flag for TLS/OCSP SCTs (Task 4.1)
5. Combine results
6. Apply CTPolicy

**Dependencies**: 1.15, 4.1, 4.2
**Acceptance Criteria**: Verifies embedded SCTs manually; checks OS flag for TLS/OCSP; produces correct VerificationResult
**Complexity**: High

---

### Task 4.4: URLSession Delegate Integration Helper

**Description**: Provide helper functions that users call from their `URLSessionDelegate`'s `didReceiveChallenge` method to perform CT verification.

**Files to create**:
- `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/urlsession/UrlSessionCtHelper.kt`

**API**:
```kotlin
public class UrlSessionCtHelper(
    private val configuration: CTConfiguration,
    private val verifier: IosCertificateTransparencyVerifier,
) {
    /**
     * Call from URLSession:didReceiveChallenge: when protection space is ServerTrust.
     * Returns the URLSession.AuthChallengeDisposition and URLCredential to use.
     */
    public fun handleServerTrustChallenge(
        challenge: NSURLAuthenticationChallenge,
    ): Pair<NSURLSessionAuthChallengeDisposition, NSURLCredential?>
}
```

**Dependencies**: 1.16, 4.3
**Acceptance Criteria**: Helper correctly evaluates trust challenges; returns appropriate disposition; fail-open/close works
**Complexity**: Medium

---

### Task 4.5: iOS Disk Cache Implementation

**Description**: Concrete `LogListCache` for iOS backed by `NSCachesDirectory`.

**Files to create**:
- `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/cache/IosDiskCache.kt`

**Dependencies**: 1.12
**Acceptance Criteria**: Persists and retrieves log list on iOS
**Complexity**: Low
