# Phase 3: seal-android — Android Integration

> **Prerequisites**: Phase 0 (Task 0.5), Phase 1A (Tasks 1.5, 1.6, 1.7), Phase 1B (Tasks 1.12, 1.15, 1.16)
> **Summary**: Android-specific integrations including Conscrypt initialization, SCT extraction from TLS extensions and OCSP responses, OkHttp network interceptor with DSL builder, X509TrustManager wrapper for WebView support, and Android disk cache.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 0 (Task 0.5), Phase 1A (Tasks 1.5, 1.6, 1.7), Phase 1B (Tasks 1.12, 1.15, 1.16).

---

## Dependency Graph (Phase 3)

```
3.1  Conscrypt initializer        ← 0.5
3.2  Conscrypt SCT extractor      ← 1.5, 3.1
3.3  OCSP response parser         ← 1.6, 1.5
3.4  Certificate chain cleaner    ← 1.7
3.5  OkHttp network interceptor   ← 1.15, 1.16, 3.2, 3.3, 3.4
3.6  OkHttp DSL builder           ← 1.16, 3.5
3.7  X509TrustManager wrapper     ← 1.15, 3.4
3.8  Android disk cache           ← 1.12
```

**Parallelization**: Tasks 3.1, 3.3, and 3.4 can be started in parallel (after their respective deps are met, they are independent of each other).

---

## Tasks

---

### Task 3.1: Conscrypt Initialization Helper

**Description**: Provide a helper to ensure Conscrypt is installed as the default security provider. Must be called early in `Application.onCreate()`.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/ConscryptInitializer.kt`

**Implementation**:
```kotlin
public object ConscryptInitializer {
    private var initialized = false
    
    public fun initialize() {
        if (!initialized) {
            Security.insertProviderAt(Conscrypt.newProvider(), 1)
            initialized = true
        }
    }
    
    public fun isConscryptSocket(socket: Socket): Boolean =
        Conscrypt.isConscrypt(socket)
}
```

**Dependencies**: 0.5
**Acceptance Criteria**: Conscrypt becomes the default provider; `Conscrypt.isConscrypt(socket)` returns true for new connections
**Complexity**: Low

---

### Task 3.2: Conscrypt SCT Extractor

**Description**: Implement SCT extraction from TLS extension data and OCSP stapled responses via Conscrypt's proprietary APIs.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/conscrypt/ConscryptSctExtractor.kt`

**Implementation**:
```kotlin
internal class ConscryptSctExtractor {
    fun extractTlsExtensionScts(socket: Socket): List<SignedCertificateTimestamp> {
        if (!Conscrypt.isConscrypt(socket)) return emptyList()
        val rawBytes = Conscrypt.getTlsSctData(socket) ?: return emptyList()
        return SctListParser.parse(rawBytes, Origin.TLS_EXTENSION)
    }
    
    fun extractOcspScts(socket: Socket): List<SignedCertificateTimestamp> {
        if (!Conscrypt.isConscrypt(socket)) return emptyList()
        val ocspBytes = Conscrypt.getOcspResponse(socket) ?: return emptyList()
        return OcspResponseParser.extractScts(ocspBytes)
    }
}
```

**Dependencies**: 1.5, 3.1
**Acceptance Criteria**: Extracts TLS SCTs from a Conscrypt socket; extracts OCSP SCTs from raw OCSP response; returns empty on non-Conscrypt sockets
**Complexity**: Medium

---

### Task 3.3: OCSP Response SCT Parser

**Description**: Parse OCSP response DER bytes to extract SCTs from the `id-pkix-ocsp-sctList` extension (OID `1.3.6.1.4.1.11129.2.4.5`).

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/conscrypt/OcspResponseParser.kt`

**Note**: Uses the ASN.1 parser from `:seal-core`. The OCSP response is a complex ASN.1 structure:
```
OCSPResponse → responseBytes → BasicOCSPResponse → tbsResponseData → responses[] → singleResponse → extensions → SCT extension
```

**Dependencies**: 1.6, 1.5
**Acceptance Criteria**: Extracts SCTs from a real OCSP response with stapled SCTs; gracefully returns empty for OCSP responses without SCTs
**Complexity**: High

---

### Task 3.4: Certificate Chain Cleaner

**Description**: Implement certificate chain ordering and cleaning. OkHttp may deliver chains in arbitrary order. The verifier needs a properly ordered chain: leaf → intermediate(s) → root.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/chain/CertificateChainCleaner.kt`

**Logic**:
1. Identify the leaf certificate (not self-signed, has the SCT extension or is the first in chain)
2. Order intermediates by subject/issuer matching
3. Optionally add the trust anchor

**Dependencies**: 1.7
**Acceptance Criteria**: Correctly orders a shuffled certificate chain; identifies leaf and intermediates
**Complexity**: Medium

---

### Task 3.5: OkHttp Network Interceptor

**Description**: Implement the main `CertificateTransparencyInterceptor` that integrates into OkHttp as a `NetworkInterceptor`. This is the primary Android integration point.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/okhttp/CertificateTransparencyInterceptor.kt`

**Workflow**:
1. `chain.proceed(request)` — establish connection
2. Check if host matches include/exclude patterns → skip if excluded
3. Extract `chain.connection()?.socket()` (must be non-null for network interceptor)
4. Get certificate chain from `chain.connection()?.handshake()?.peerCertificates()`
5. Extract embedded SCTs from leaf cert
6. Extract TLS extension SCTs (via ConscryptSctExtractor)
7. Extract OCSP SCTs (via ConscryptSctExtractor)
8. Clean certificate chain
9. Run `CertificateTransparencyVerifier.verify()`
10. If failure AND `failOnError` → throw `SSLPeerUnverifiedException`
11. If failure AND !`failOnError` → log warning, return response
12. Invoke logger callback with result
13. Return response

**Dependencies**: 1.15, 1.16, 3.2, 3.3, 3.4
**Acceptance Criteria**: Intercepts HTTPS connections; extracts SCTs from all 3 delivery methods; applies policy; fail-open/close works correctly; logger invoked
**Complexity**: High

---

### Task 3.6: OkHttp DSL Builder

**Description**: Provide the top-level `certificateTransparencyInterceptor { }` DSL function.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/okhttp/CertificateTransparencyDsl.kt`

**API**:
```kotlin
public fun certificateTransparencyInterceptor(
    block: CTConfigurationBuilder.() -> Unit = {}
): Interceptor {
    val config = CTConfigurationBuilder().apply(block).build()
    return CertificateTransparencyInterceptor(config)
}
```

**Dependencies**: 1.16, 3.5
**Acceptance Criteria**: DSL compiles and produces a working Interceptor; matches target API design
**Complexity**: Low

---

### Task 3.7: X509TrustManager Wrapper

**Description**: Implement a wrapping `X509TrustManager` that adds CT verification on top of the system's default trust manager. This enables CT enforcement for WebViews and other non-OkHttp connections.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/trust/CTTrustManager.kt`
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/trust/CTTrustManagerFactory.kt`

**Logic**:
1. Delegate normal certificate validation to the system `X509TrustManager`
2. After successful system validation, perform CT verification on the chain
3. Note: This only has access to **embedded SCTs** (no TLS extension or OCSP from TrustManager context)

**Dependencies**: 1.15, 3.4
**Acceptance Criteria**: Wraps system trust manager; performs CT check after normal validation; throws `CertificateException` on CT failure in fail-closed mode
**Complexity**: Medium

---

### Task 3.8: Android Disk Cache Implementation

**Description**: Concrete `LogListCache` implementation backed by Android's cache directory with `Context`.

**Files to create**:
- `seal-android/src/androidMain/kotlin/com/jermey/seal/android/cache/AndroidDiskCache.kt`

**Implementation**: Uses `context.cacheDir / "seal-ct-loglist"`. Stores JSON + metadata file (timestamp, ETag, last-modified).

**Dependencies**: 1.12
**Acceptance Criteria**: Persists and retrieves log list; handles corrupt files gracefully; respects cache expiry
**Complexity**: Low
