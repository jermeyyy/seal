# Phase 1B: seal-core — Crypto, Verification & Policy

> **Prerequisites**: Phase 0 and Phase 1A tasks (especially 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7)
> **Summary**: These tasks build the verification engine, log list management, policy engine, and configuration DSL. This includes the CryptoVerifier interface, SCT signature verification, V3 log list parsing and caching, Chrome/Apple policy presets, the core verification orchestrator, and the DSL builder base.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 0 and Phase 1A tasks (especially 1.1, 1.2, 1.3, 1.4, 1.5, 1.6, 1.7).

### Specific Dependency Chain for Phase 1B Tasks

- **1.8** (CryptoVerifier interface) ← 0.4
- **1.9** (SCT signature verifier) ← 1.1, 1.7, 1.8
- **1.10** (Log list V3 JSON parser) ← 0.4, 1.3
- **1.11** (Log list sig verification) ← 1.8, 1.10
- **1.12** (Log list service abstraction) ← 1.10, 1.11
- **1.13** (Embedded baseline log list) ← 1.10
- **1.14** (Policy engine) ← 1.2, 1.3
- **1.15** (Core verification orchestrator) ← 1.5, 1.7, 1.9, 1.12, 1.14
- **1.16** (DSL builder base) ← 1.4, 1.14

---

## Dependency Graph (Phase 1B)

```
1.8  CryptoVerifier interface     ← 0.4
1.9  SCT signature verifier       ← 1.1, 1.7, 1.8
1.10 Log list V3 JSON parser      ← 0.4, 1.3
1.11 Log list sig verification    ← 1.8, 1.10
1.12 Log list service abstraction ← 1.10, 1.11
1.13 Embedded baseline log list   ← 1.10
1.14 Policy engine                ← 1.2, 1.3
1.15 Core verification orch.      ← 1.5, 1.7, 1.9, 1.12, 1.14
1.16 DSL builder base             ← 1.4, 1.14
```

**Parallelization**: Tasks 1.8, 1.10, and 1.14 can be started in parallel once their respective Phase 1A dependencies are met.

---

## Tasks

---

### Task 1.8: Define CryptoVerifier Interface

**Description**: Define the `expect`/`actual` interface for cryptographic operations needed by the CT verification engine.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/crypto/CryptoVerifier.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/crypto/CryptoProvider.kt`

**Interface**:
```kotlin
public interface CryptoVerifier {
    /**
     * Verify a digital signature.
     * @param publicKeyBytes DER-encoded SubjectPublicKeyInfo
     * @param data The signed data
     * @param signature The signature bytes
     * @param algorithm Signature algorithm (SHA256withECDSA or SHA256withRSA)
     */
    public fun verifySignature(
        publicKeyBytes: ByteArray,
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm,
    ): Boolean

    /** SHA-256 hash */
    public fun sha256(data: ByteArray): ByteArray
}

// Factory
public expect fun createCryptoVerifier(): CryptoVerifier
```

**Dependencies**: 0.4
**Acceptance Criteria**: Interface compiles; expect declaration present in commonMain
**Complexity**: Low

---

### Task 1.9: Implement SCT Signature Verification

**Description**: Implement the core logic that verifies an individual SCT's signature against a log's public key. This involves reconstructing the signed data structure per RFC 6962 §3.2.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/verification/SctSignatureVerifier.kt`

**Signed data structure** (RFC 6962 §3.2):
```
digitally-signed struct {
    Version sct_version;
    SignatureType signature_type = certificate_timestamp;
    uint64 timestamp;
    LogEntryType entry_type;       // x509_entry or precert_entry
    select(entry_type) {
        case x509_entry: opaque ASN.1Cert<1..2^24-1>;
        case precert_entry:
            opaque issuer_key_hash[32];
            opaque TBSCertificate<1..2^24-1>;
    } signed_entry;
    CtExtensions extensions;
} SignedCertificateTimestamp;
```

**Logic**:
1. Determine entry type (x509 or precert based on precert poison extension)
2. Build the signed data per the struct above
3. Use CryptoVerifier to verify signature
4. Return `SctVerificationResult`

**Dependencies**: 1.1, 1.7, 1.8
**Acceptance Criteria**: Correctly verifies SCTs from real certificates against known log public keys; rejects tampered SCTs
**Complexity**: High

---

### Task 1.10: Implement Log List V3 JSON Parser

**Description**: Parse the Google CT Log List V3 JSON schema using `kotlinx-serialization` with lenient/defensive configuration. Must tolerate empty `logs` arrays, unknown fields, and `tiled_logs` entries.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListDto.kt` (serialization models)
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListParser.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListMapper.kt`

**V3 Schema** (key parts):
```json
{
  "version": "3",
  "log_list_timestamp": "2025-12-01T00:00:00Z",
  "operators": [
    {
      "name": "Google",
      "email": ["..."],
      "logs": [
        {
          "description": "...",
          "log_id": "base64...",
          "key": "base64...",
          "url": "https://...",
          "mmd": 86400,
          "state": { "usable": { "timestamp": "..." } },
          "temporal_interval": { "start_inclusive": "...", "end_exclusive": "..." }
        }
      ],
      "tiled_logs": [ ... ]  // newer format, may exist alongside or instead of "logs"
    }
  ]
}
```

**Serialization config**:
```kotlin
val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    coerceInputValues = true
    explicitNulls = false
}
```

**Key requirement**: MUST NOT crash if `logs` is empty or missing. Extract keys from both `logs` and `tiled_logs`.

**Dependencies**: 0.4, 1.3
**Acceptance Criteria**: Parses real V3 log list JSON; handles operators with empty `logs`; handles `tiled_logs`; maps to domain `LogServer` models
**Complexity**: Medium

---

### Task 1.11: Implement Log List Signature Verification

**Description**: Verify the digital signature of the log list JSON file using Google's known public key. The signature is in a separate `.sig` file.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListSignatureVerifier.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/GoogleLogListPublicKey.kt`

**Logic**:
1. Fetch `log_list.json` and `log_list.sig` (signature file)
2. Verify ECDSA signature of the JSON bytes against Google's embedded public key
3. Reject tampered log lists

**Dependencies**: 1.8, 1.10
**Acceptance Criteria**: Verifies authentic log list; rejects modified JSON
**Complexity**: Medium

---

### Task 1.12: Implement Log List Service Abstraction

**Description**: Define the log list loading, caching, and update abstractions. This is the orchestration layer that manages the lifecycle of the trusted log list.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListService.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListDataSource.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/LogListCache.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/InMemoryLogListCache.kt`

**Interfaces**:
```kotlin
public interface LogListDataSource {
    public suspend fun fetchLogList(): Result<RawLogList>
}

public interface LogListCache {
    public suspend fun get(): CachedLogList?
    public suspend fun put(logList: CachedLogList)
}

public class LogListService(
    private val networkSource: LogListDataSource,
    private val embeddedSource: LogListDataSource,
    private val cache: LogListCache,
    private val signatureVerifier: LogListSignatureVerifier,
    private val maxAge: Duration = 10.weeks,
) {
    public suspend fun getLogList(): LogListResult
}
```

**Loading priority**: Cache → Network → Embedded fallback
**Staleness**: If cached list is > `maxAge`, attempt network refresh. If that fails, use stale cache with warning. If no cache, use embedded.

**Dependencies**: 1.10, 1.11
**Acceptance Criteria**: Returns log list from cache if fresh; fetches from network if stale; falls back to embedded if all else fails; never crashes
**Complexity**: Medium

---

### Task 1.13: Bundle Embedded Baseline Log List

**Description**: Download the current V3 log list JSON file, compress it (gzip), and bundle it as a resource in the `seal-core` commonMain resources. Provide a loader.

**Files to create**:
- `seal-core/src/commonMain/composeResources/files/log_list.json` (or `resources/` processed via `kotlinx-io`)
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/EmbeddedLogListDataSource.kt`

**Approach**: Use Kotlin Multiplatform resources (Compose resources or `expect`/`actual` resource loading).

**Dependencies**: 1.10
**Acceptance Criteria**: Embedded log list loads correctly on both Android and iOS; parses without errors
**Complexity**: Medium

---

### Task 1.14: Implement Policy Engine

**Description**: Define the `CTPolicy` interface and implement Chrome and Apple policy presets. The policy determines how many valid SCTs from how many distinct operators are required.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/policy/CTPolicy.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/policy/ChromeCtPolicy.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/policy/AppleCtPolicy.kt`

**Interface**:
```kotlin
public fun interface CTPolicy {
    /**
     * Evaluate whether the given SCT verification results satisfy this policy.
     * @param certificateLifetimeDays Validity period of the leaf certificate in days
     * @param sctResults Results of individual SCT verifications
     * @return VerificationResult indicating policy compliance
     */
    public fun evaluate(
        certificateLifetimeDays: Long,
        sctResults: List<SctVerificationResult>,
    ): VerificationResult
}
```

**Chrome Policy** (approximate):
| Certificate Lifetime | Required SCTs |
|----------------------|---------------|
| < 180 days           | ≥ 2           |
| ≥ 180 days           | ≥ 3           |
| Plus: at least 1 SCT from a Google log and 1 from a non-Google log (operator diversity)

**Apple Policy** (approximate):
| Certificate Lifetime | Required SCTs |
|----------------------|---------------|
| < 15 months          | ≥ 2           |
| 15–27 months         | ≥ 3           |
| 27–39 months         | ≥ 4           |
| > 39 months          | ≥ 5           |
| Plus: at least 1 SCT from once-or-currently qualified log

**Dependencies**: 1.2, 1.3
**Acceptance Criteria**: Both policies correctly map cert lifetime to SCT requirements; operator diversity enforced; unit tests with edge cases
**Complexity**: Medium

---

### Task 1.15: Implement Core Verification Orchestrator

**Description**: Build the main verification engine that ties together all components: SCT extraction, signature verification, log list lookup, and policy evaluation.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/verification/CertificateTransparencyVerifier.kt`

**Input**: Certificate chain (DER bytes), optional TLS extension SCT bytes, optional OCSP response bytes
**Output**: `VerificationResult`

**Steps**:
1. Extract embedded SCTs from leaf certificate
2. Parse TLS extension SCTs (if provided)
3. Parse OCSP response SCTs (if provided)
4. Merge all SCTs with their `Origin`
5. For each SCT, look up the log in the trusted log list
6. Verify each SCT's signature
7. Apply the configured `CTPolicy`
8. Return `VerificationResult`

**Dependencies**: 1.5, 1.7, 1.9, 1.12, 1.14
**Acceptance Criteria**: End-to-end verification succeeds with real certificate chain; correct handling of all SCT origins; policy applied correctly
**Complexity**: High

---

### Task 1.16: Implement DSL Builder Base

**Description**: Create the base DSL builder used by both OkHttp and Ktor integrations. This defines the common configuration surface.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/config/CTConfiguration.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/config/CTConfigurationBuilder.kt`

**Builder DSL**:
```kotlin
public class CTConfigurationBuilder {
    // Host include/exclude
    public operator fun String.unaryPlus()  // include host
    public operator fun String.unaryMinus() // exclude host

    // Policy
    public var policy: CTPolicy
    
    // Failure mode
    public var failOnError: Boolean  // false = fail-open (default)
    
    // Logger callback
    public var logger: ((host: String, result: VerificationResult) -> Unit)?
    
    // Log list
    public var logListUrl: String
    
    // Cache
    public var logListCache: LogListCache?
    
    internal fun build(): CTConfiguration
}
```

**Dependencies**: 1.4, 1.14
**Acceptance Criteria**: DSL compiles and produces correct CTConfiguration; `+`/`-` operators work for host patterns
**Complexity**: Medium
