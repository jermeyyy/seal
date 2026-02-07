# Phase 7: Testing

> **Prerequisites**: Various — each test depends on its corresponding implementation task (see per-task dependencies below)
> **Summary**: Comprehensive test suite covering SCT parsing, ASN.1 parsing, X.509 extension extraction, log list parsing, policy evaluation, host matching, signature verification, end-to-end verification, OkHttp interceptor integration, and log list service fallback scenarios.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Each test task depends on its corresponding implementation task. Full mapping:

| Test Task | Tests For | Depends On |
|-----------|-----------|------------|
| 7.1 | SCT Deserializer | 1.5 |
| 7.2 | ASN.1 Parser | 1.6 |
| 7.3 | X.509 Extension Extraction | 1.7 |
| 7.4 | Log List Parser | 1.10, 1.11 |
| 7.5 | Policy Engine | 1.14 |
| 7.6 | Host Matcher | 1.4 |
| 7.7 | SCT Signature Verification | 1.9, 2.1/2.2 |
| 7.8 | End-to-End Verification | 1.15 |
| 7.9 | OkHttp Interceptor Integration | 3.5 |
| 7.10 | Mock Log List Service | 1.12 |

---

## Testing Strategy

Tests for each component can begin as soon as that component's implementation is complete. There is no need to wait for all implementation to finish before starting tests.

### Parallelization Opportunities

From the dependency graph:

| Can be done in parallel |
|--------------------------------------------------------------|
| 7.1..7.6 (can start as soon as respective code is done) |

Specifically:
- **7.1** (SCT deserializer tests) can start after Task 1.5
- **7.2** (ASN.1 parser tests) can start after Task 1.6
- **7.3** (X.509 extraction tests) can start after Task 1.7
- **7.4** (Log list parser tests) can start after Tasks 1.10, 1.11
- **7.5** (Policy engine tests) can start after Task 1.14
- **7.6** (Host matcher tests) can start after Task 1.4
- **7.7** (SCT signature tests) can start after Tasks 1.9 + 2.1/2.2
- **7.8** (E2E verification tests) can start after Task 1.15
- **7.9** (OkHttp interceptor tests) can start after Task 3.5
- **7.10** (Mock log list tests) can start after Task 1.12

---

## Dependency Graph (Phase 7)

```
7.1  SCT deserializer tests       ← 1.5
7.2  ASN.1 parser tests           ← 1.6
7.3  X.509 extraction tests       ← 1.7
7.4  Log list parser tests        ← 1.10, 1.11
7.5  Policy engine tests          ← 1.14
7.6  Host matcher tests           ← 1.4
7.7  SCT signature tests          ← 1.9, 2.1/2.2
7.8  E2E verification tests       ← 1.15
7.9  OkHttp interceptor tests     ← 3.5
7.10 Mock log list tests          ← 1.12
```

---

## Tasks

---

### Task 7.1: SCT Deserializer Unit Tests

**Description**: Test the binary SCT parser with real and crafted test vectors.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/parser/SctDeserializerTest.kt`
- `seal-core/src/commonTest/resources/test-scts/` (test vectors)

**Test cases**:
- Parse single SCT from known bytes
- Parse SCT list (multiple SCTs)
- Handle truncated data (graceful failure)
- Handle empty input
- Verify all fields match expected values
- Round-trip: known log ID, timestamp, signature

**Dependencies**: 1.5
**Acceptance Criteria**: All tests pass; covers happy path and error cases
**Complexity**: Medium

---

### Task 7.2: ASN.1 Parser Unit Tests

**Description**: Test the ASN.1 DER parser with various structures.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/asn1/Asn1ParserTest.kt`

**Test cases**:
- Parse primitive types (INTEGER, OID, OCTET_STRING, BIT_STRING, etc.)
- Parse SEQUENCE / SET
- Parse nested structures
- OID encoding/decoding
- Context-specific tags
- Extract extensions from real X.509 certificate DER
- Handle malformed DER

**Dependencies**: 1.6
**Acceptance Criteria**: Parser correctly handles all ASN.1 types needed for X.509/OCSP
**Complexity**: Medium

---

### Task 7.3: X.509 Extension Extraction Tests

**Description**: Test embedded SCT extraction from real certificates.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/x509/CertificateParserTest.kt`
- `seal-core/src/commonTest/resources/test-certs/` (DER certificates with embedded SCTs)

**Test cases**:
- Extract SCTs from a certificate with embedded SCTs (e.g., google.com)
- Detect precertificate (poison extension)
- Handle certificate without SCTs
- TBS certificate reconstruction

**Dependencies**: 1.7
**Acceptance Criteria**: Correctly extracts SCTs from real certificates
**Complexity**: Medium

---

### Task 7.4: Log List Parser Tests

**Description**: Test V3 JSON log list parsing robustness.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/loglist/LogListParserTest.kt`
- `seal-core/src/commonTest/resources/test-loglist/` (JSON fixtures)

**Test cases**:
- Parse full real V3 log list
- Parse operator with empty `logs` array (appmattus crash case)
- Parse operator with only `tiled_logs`
- Parse with unknown fields (forward compatibility)
- Handle null/missing optional fields
- Signature verification with valid/invalid signatures

**Dependencies**: 1.10, 1.11
**Acceptance Criteria**: Parses all valid V3 variants; never crashes on unexpected input
**Complexity**: Medium

---

### Task 7.5: Policy Engine Tests

**Description**: Test Chrome and Apple policy evaluation with various SCT combinations.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/policy/ChromeCtPolicyTest.kt`
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/policy/AppleCtPolicyTest.kt`

**Test cases**:
- Short-lived cert with 2 SCTs → pass
- Long-lived cert with 2 SCTs → fail (need 3)
- Enough SCTs but insufficient operator diversity → fail
- SCTs from rejected logs → don't count
- All edge cases from Chrome and Apple policy tables
- Custom policy implementation

**Dependencies**: 1.14
**Acceptance Criteria**: All policy edge cases covered; correct pass/fail for each scenario
**Complexity**: Medium

---

### Task 7.6: Host Matcher Tests

**Description**: Test hostname pattern matching.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/host/HostMatcherTest.kt`

**Test cases**:
- `"*.example.com"` matches `api.example.com`, not `example.com`
- `"**.example.com"` matches `deep.sub.example.com`
- Exclude overrides include
- No includes → match all
- Case insensitivity
- IP address handling

**Dependencies**: 1.4
**Acceptance Criteria**: All wildcard and precedence cases pass
**Complexity**: Low

---

### Task 7.7: SCT Signature Verification Tests

**Description**: Integration test: verify real SCT signatures.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/verification/SctSignatureVerifierTest.kt`

**Test cases**:
- Verify an SCT from a known log against the log's public key
- Reject an SCT with tampered timestamp
- Reject an SCT with wrong log key
- Both ECDSA and RSA signatures (if test vectors available)

**Dependencies**: 1.9, 2.1 (or 2.2 for iOS)
**Acceptance Criteria**: Real SCT verification succeeds; tampered SCTs rejected
**Complexity**: Medium

---

### Task 7.8: End-to-End Verification Tests

**Description**: Full pipeline tests: certificate chain → SCT extraction → signature verification → policy evaluation.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/verification/CertificateTransparencyVerifierTest.kt`

**Test cases**:
- Full verification of a google.com certificate chain
- Certificate with no SCTs → `Failure.NoScts`
- Certificate with expired log SCTs → `Failure.LogServersFailed`
- Verification with stale but valid embedded log list
- Custom policy evaluation

**Dependencies**: 1.15
**Acceptance Criteria**: End-to-end verification matches expected results
**Complexity**: High

---

### Task 7.9: OkHttp Interceptor Integration Tests

**Description**: Test the OkHttp interceptor with `MockWebServer` or real HTTPS endpoints.

**Files to create**:
- `seal-android/src/test/kotlin/com/jermey/seal/android/okhttp/CertificateTransparencyInterceptorTest.kt`

**Test cases**:
- Interceptor passes valid CT connections
- Interceptor reports failures in fail-open mode
- Interceptor blocks failures in fail-closed mode
- Host include/exclude filtering works
- Logger callback invoked
- Non-HTTPS connections pass through

**Dependencies**: 3.5
**Acceptance Criteria**: Interceptor correctly handles all scenarios in automated tests
**Complexity**: High

---

### Task 7.10: Mock Log List Tests

**Description**: Test the log list service with mocked network responses.

**Files to create**:
- `seal-core/src/commonTest/kotlin/com/jermey/seal/core/loglist/LogListServiceTest.kt`

**Test cases**:
- Fresh cache → return cached list
- Stale cache → fetch from network → update cache
- Network failure → fall back to stale cache
- No cache, network failure → fall back to embedded
- Corrupt cache → fall back to network/embedded
- Signature verification failure on network response → fall back to cached/embedded

**Dependencies**: 1.12
**Acceptance Criteria**: All fallback scenarios work correctly; service never crashes
**Complexity**: Medium
