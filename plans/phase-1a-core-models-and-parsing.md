# Phase 1A: seal-core — Data Models & Parsers

> **Prerequisites**: Phase 0 (specifically Task 0.4 — seal-core build.gradle.kts)
> **Summary**: These tasks create the foundational data models and parsing infrastructure in commonMain. This includes SCT data models, verification result types, log server models, host pattern matching, binary SCT parsing, ASN.1 DER parsing, and X.509 certificate extension extraction.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 0 (specifically Task 0.4) to be complete.

## What Comes Next

Phase 1B builds crypto, verification, log list management, and policy on top of these models and parsers.

---

## Dependency Graph (Phase 1A)

```
1.1  SCT data models              ← 0.4
1.2  VerificationResult models    ← 1.1
1.3  Log server models            ← 1.1
1.4  Host pattern matching        ← 0.4
1.5  SCT deserializer             ← 1.1
1.6  ASN.1 DER parser             ← 0.4
1.7  X.509 extension parser       ← 1.5, 1.6
```

**Parallelization**: Tasks 1.1, 1.4, and 1.6 all depend only on 0.4 and can be done in parallel.

---

## Tasks

---

### Task 1.1: Define SCT Data Models

**Description**: Create the core data model classes that represent Certificate Transparency primitives. All classes must be immutable data classes with clear `public`/`internal` visibility.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/SignedCertificateTimestamp.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/DigitallySigned.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/LogId.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/SctVersion.kt`

**Types to define**:
```kotlin
// SctVersion.kt
public enum class SctVersion(public val value: Int) { V1(0) }

// LogId.kt
public data class LogId(val keyId: ByteArray) // 32 bytes, SHA-256 of log public key

// DigitallySigned.kt
public data class DigitallySigned(
    val hashAlgorithm: HashAlgorithm,
    val signatureAlgorithm: SignatureAlgorithm,
    val signature: ByteArray,
)

public enum class HashAlgorithm(public val value: Int) { NONE(0), MD5(1), SHA1(2), SHA224(3), SHA256(4), SHA384(5), SHA512(6) }
public enum class SignatureAlgorithm(public val value: Int) { ANONYMOUS(0), RSA(1), DSA(2), ECDSA(3) }

// SignedCertificateTimestamp.kt
public data class SignedCertificateTimestamp(
    val version: SctVersion,
    val logId: LogId,
    val timestamp: Instant,  // kotlinx-datetime
    val extensions: ByteArray,
    val signature: DigitallySigned,
    val origin: Origin,
)

public enum class Origin { EMBEDDED, TLS_EXTENSION, OCSP_RESPONSE }
```

**Dependencies**: 0.4
**Acceptance Criteria**: Models compile in commonMain; all properly annotated with `public`/`internal`
**Complexity**: Low

---

### Task 1.2: Define Verification Result Models

**Description**: Create the sealed class hierarchies for verification results at both the individual SCT level and the overall connection level.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/VerificationResult.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/SctVerificationResult.kt`

**Types**:
```kotlin
// SctVerificationResult.kt
public sealed class SctVerificationResult {
    public data class Valid(
        val sct: SignedCertificateTimestamp,
        val logOperator: String?,
    ) : SctVerificationResult()

    public sealed class Invalid : SctVerificationResult() {
        public data class FailedVerification(val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogNotTrusted(val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogExpired(val sct: SignedCertificateTimestamp) : Invalid()
        public data class LogRejected(val sct: SignedCertificateTimestamp) : Invalid()
        public data class SignatureMismatch(val sct: SignedCertificateTimestamp) : Invalid()
    }
}

// VerificationResult.kt
public sealed class VerificationResult {
    public sealed class Success : VerificationResult() {
        public data class Trusted(val validScts: List<SctVerificationResult.Valid>) : Success()
        public data object InsecureConnection : Success()
        public data object DisabledForHost : Success()
        public data object DisabledStaleLogList : Success()
    }
    public sealed class Failure : VerificationResult() {
        public data object NoScts : Failure()
        public data class TooFewSctsTrusted(val found: Int, val required: Int) : Failure()
        public data class TooFewDistinctOperators(val found: Int, val required: Int) : Failure()
        public data class LogServersFailed(val sctResults: List<SctVerificationResult>) : Failure()
        public data class UnknownError(val cause: Throwable) : Failure()
    }
}
```

**Dependencies**: 1.1
**Acceptance Criteria**: Sealed hierarchies compile; exhaustive `when` is possible
**Complexity**: Low

---

### Task 1.3: Define Log Server Models

**Description**: Create data models representing CT Log servers and operators, matching the V3 log list schema.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/LogServer.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/LogOperator.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/model/LogState.kt`

**Types**:
```kotlin
public data class LogServer(
    val logId: LogId,
    val publicKey: ByteArray,
    val operator: String,
    val url: String,
    val state: LogState,
    val temporalInterval: TemporalInterval?, // valid timestamp range
)

public enum class LogState { PENDING, QUALIFIED, USABLE, READ_ONLY, RETIRED, REJECTED }

public data class LogOperator(
    val name: String,
    val logs: List<LogServer>,
)

public data class TemporalInterval(
    val startInclusive: Instant,
    val endExclusive: Instant,
)
```

**Dependencies**: 1.1
**Acceptance Criteria**: Models compile and can represent all V3 log list entries
**Complexity**: Low

---

### Task 1.4: Define Host Pattern Matching

**Description**: Implement hostname include/exclude matching with wildcard support. The `+` and `-` DSL operators add hosts to include/exclude lists. If no includes are specified, all hosts are included. Excludes take precedence.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/host/HostPattern.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/host/HostMatcher.kt`

**Key behavior**:
- `"*.example.com"` matches `api.example.com`, `www.example.com` but NOT `example.com`
- `"**"` or no includes → match everything
- Excludes override includes
- Support nested wildcards: `"**.example.com"` matches any depth

**Dependencies**: 0.4
**Acceptance Criteria**: Unit tests pass for wildcard matching, include/exclude logic, edge cases
**Complexity**: Medium

---

### Task 1.5: Implement Binary SCT Deserializer

**Description**: Implement the RFC 6962 binary SCT parser that can deserialize SCTs from raw byte arrays. Must handle both single SCTs and SCT lists (as delivered in X.509 extensions and TLS extensions).

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/parser/SctDeserializer.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/parser/SctListParser.kt`

**Format to parse** (RFC 6962 §3.3):
```
struct {
    Version sct_version;          // 1 byte
    LogID id;                     // 32 bytes  
    uint64 timestamp;             // 8 bytes (ms since epoch)
    opaque extensions<0..2^16-1>; // 2-byte length prefix + data
    DigitallySigned signature;    // hash_alg(1) + sig_alg(1) + sig_len(2) + sig
} SignedCertificateTimestamp;
```

**SCT list format** (for embedded/TLS extension):
```
opaque SerializedSCT<1..2^16-1>;  // 2-byte length prefix per SCT
struct {
    SerializedSCT sct_list<1..2^16-1>;  // 2-byte total length prefix
} SignedCertificateTimestampList;
```

**Dependencies**: 1.1
**Acceptance Criteria**: Parser correctly deserializes SCTs from real certificate test vectors; handles malformed input gracefully (returns error, no crash)
**Complexity**: Medium

---

### Task 1.6: Implement ASN.1 DER Parser

**Description**: Implement a lightweight, streaming ASN.1 DER parser in pure Kotlin commonMain. This is critical infrastructure for X.509 certificate parsing, precertificate handling, and OCSP response parsing.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/asn1/Asn1Element.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/asn1/Asn1Parser.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/asn1/Asn1Tag.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/asn1/Oid.kt`

**Capabilities required**:
- Parse TAG + LENGTH + VALUE (primitive and constructed types)
- Universal tags: SEQUENCE, SET, OCTET_STRING, BIT_STRING, INTEGER, OID, UTF8String, PrintableString, UTCTime, GeneralizedTime, BOOLEAN
- Context-specific tags (IMPLICIT/EXPLICIT tagging)
- OID encoding/decoding
- Navigate into nested structures
- Extract raw bytes for specific elements
- Memory-efficient: streaming/lazy where possible

**No need to support**: BER encoding, indefinite lengths, SET OF ordering. Read-only (no ASN.1 generation).

**Dependencies**: 0.4
**Acceptance Criteria**: Can parse a real X.509 certificate DER, extract extensions by OID, handle nested CONSTRUCTED types
**Complexity**: High

---

### Task 1.7: Implement X.509 Certificate Extension Parser

**Description**: Build on the ASN.1 parser to extract specific X.509 extensions relevant to CT: embedded SCTs, precertificate poison extension, and precertificate signing certificate EKU.

**Files to create**:
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/x509/CertificateParser.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/x509/CertificateExtensions.kt`
- `seal-core/src/commonMain/kotlin/com/jermey/seal/core/x509/TbsCertificateBuilder.kt`

**Key OIDs**:
- SCT Extension: `1.3.6.1.4.1.11129.2.4.2`
- Precert Poison: `1.3.6.1.4.1.11129.2.4.3`
- Precert Signing Cert EKU: `1.3.6.1.4.1.11129.2.4.4`

**Functionality**:
- Extract embedded SCTs from a certificate's extension bytes
- Detect precertificates (presence of poison extension)
- Reconstruct TBS certificate for SCT signature verification (remove poison extension, replace issuer key hash if precert)
- Extract issuer public key hash from certificate

**Dependencies**: 1.5, 1.6
**Acceptance Criteria**: Can extract embedded SCTs from a real certificate; correctly identifies precertificates; TBS reconstruction matches expected hash
**Complexity**: High
