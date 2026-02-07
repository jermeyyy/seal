# Seal — KMP Certificate Transparency Library: Architecture Overview

> **This document provides shared architectural context for all phases. Read this first before working on any phase.**

> **Version**: 1.0
> **Created**: 2026-02-07
> **Source**: Extracted from [seal-implementation-plan.md](seal-implementation-plan.md)

---

## Overview

Transform the existing Compose Multiplatform template project into a multi-module KMP Certificate Transparency library that:

- Verifies SCTs across all 3 delivery mechanisms (Embedded X.509, TLS Extension, OCSP Stapling)
- Supports Android (API 24+) via Conscrypt and iOS via Security.framework
- Provides OkHttp interceptor, Ktor plugin, and platform integration helpers
- Ships with configurable policies (Chrome, Apple presets + custom)
- Defaults to fail-open with reporting callbacks
- Is ready for Maven Central publishing

### Current State

| Property            | Value                                       |
|---------------------|---------------------------------------------|
| Kotlin              | 2.3.0                                       |
| Compose MP          | 1.10.0                                      |
| AGP                 | 8.11.2                                      |
| Android minSdk      | 24                                          |
| Android compileSdk  | 36                                          |
| Targets             | Android, iosArm64, iosSimulatorArm64        |
| Modules             | `composeApp` (single application module)    |
| Serialization/Ktor  | Not configured                              |

### Target State

| Module          | Type       | Targets                            | Purpose                                    |
|-----------------|------------|------------------------------------|--------------------------------------------|
| `:seal-core`    | KMP lib    | Android, iosArm64, iosSimulatorArm64 | Data models, parsing, verification, policy |
| `:seal-android` | Android lib| Android only                       | Conscrypt, OkHttp, TrustManager            |
| `:seal-ios`     | KMP lib    | iosArm64, iosSimulatorArm64       | SecTrust, URLSession helpers               |
| `:seal-ktor`    | KMP lib    | Android, iosArm64, iosSimulatorArm64 | Ktor plugin wrapping platform logic        |
| `:composeApp`   | KMP app    | Android, iosArm64, iosSimulatorArm64 | Demo / test app                            |

---

## Module Dependency Diagram

```
┌─────────────────────────────────────────────────────┐
│                    :composeApp                       │
│              (Demo / Test Application)               │
│        depends on: seal-android, seal-ios,           │
│                    seal-ktor, seal-core              │
└──────────┬──────────────┬──────────────┬─────────────┘
           │              │              │
           ▼              ▼              ▼
   ┌──────────────┐ ┌──────────┐ ┌──────────────┐
   │ :seal-android│ │:seal-ktor│ │  :seal-ios   │
   │              │ │          │ │              │
   │ Conscrypt    │ │ Ktor     │ │ SecTrust     │
   │ OkHttp       │ │ Plugin   │ │ URLSession   │
   │ TrustManager │ │          │ │ helpers      │
   └──────┬───────┘ └────┬─────┘ └──────┬───────┘
          │               │              │
          └───────────────┼──────────────┘
                          │
                          ▼
                  ┌──────────────┐
                  │  :seal-core  │
                  │              │
                  │ Data Models  │
                  │ SCT Parser   │
                  │ ASN.1 Parser │
                  │ Crypto iface │
                  │ Log List     │
                  │ Policy       │
                  │ Host Match   │
                  └──────────────┘
```

### External Dependencies per Module

| Module          | External Dependencies                                                              |
|-----------------|-------------------------------------------------------------------------------------|
| `:seal-core`    | `kotlinx-serialization-json`, `kotlinx-datetime`, `kotlinx-io-core`                |
| `:seal-android` | `:seal-core`, `conscrypt-android`, `okhttp3`, `kotlinx-coroutines-android`          |
| `:seal-ios`     | `:seal-core`                                                                        |
| `:seal-ktor`    | `:seal-core`, `ktor-client-core`; optionally `:seal-android`, `:seal-ios` (expect/actual) |
| `:composeApp`   | All library modules, Compose Multiplatform, Ktor client engines                     |

---

## Key Design Decisions

### KD-1: Fail-Open Default
**Decision**: Default to fail-open (allow connections, emit warnings) rather than fail-closed.
**Rationale**: Prevents app breakage from log list staleness, network errors, or schema changes. Mirrors the appmattus lesson — strict enforcement caused app-wide outages. Users can override to fail-closed.

### KD-2: Conscrypt as Mandatory Android Dependency
**Decision**: Require `org.conscrypt:conscrypt-android` in `:seal-android`.
**Rationale**: Only way to access TLS extension SCTs and OCSP stapled responses on Android < 16. Without it, only embedded SCTs are verifiable. Conscrypt is ~3 MB but provides consistent TLS behavior across API levels 24-35.

### KD-3: Lenient JSON Parsing with kotlinx-serialization
**Decision**: Use `ignoreUnknownKeys = true`, coerce defaults, and tolerate empty arrays in log list parsing.
**Rationale**: Directly prevents the appmattus crash caused by `require(logs.isNotEmpty())` when Google added `tiled_logs`. Future schema evolution must not crash the library.

### KD-4: Hybrid iOS Verification
**Decision**: Manually verify embedded SCTs on iOS. For TLS extension and OCSP SCTs, delegate to OS via `kSecTrustCertificateTransparency` flag.
**Rationale**: NSURLSession does not expose raw TLS extension data or OCSP response bytes. Rewriting networking via low-level NWConnection is impractical and incompatible with Ktor's Darwin engine.

### KD-5: Pure Kotlin ASN.1 Parser
**Decision**: Implement a lightweight, read-only ASN.1 DER parser in commonMain. No Bouncy Castle dependency.
**Rationale**: Required for X.509 certificate extension parsing (SCT extraction, precertificate detection, TBS manipulation). Must work in common code for both Android and iOS. Keeps binary size minimal.

### KD-6: Embedded Baseline Log List
**Decision**: Bundle a compressed `log_list.json` as a library resource. Update at each library release.
**Rationale**: Guarantees the library works immediately without network access. Acts as fallback when remote fetch fails. Prevents the zero-day-broken-list scenario.

### KD-7: Configurable Log List URL
**Decision**: Default to Chrome V3 (`https://www.gstatic.com/ct/log_list/v3/log_list.json`) but allow users to configure alternatives.
**Rationale**: Chrome's endpoint is not stable for third parties. Users can point to enterprise mirrors, Apple's list, or custom endpoints.

### KD-8: CryptoVerifier expect/actual Pattern
**Decision**: Define `CryptoVerifier` as an `expect` interface (or `expect` factory) in commonMain with `actual` implementations per platform.
**Rationale**: CT signature verification requires ECDSA/RSA — no pure-Kotlin cross-platform crypto is available. Android uses `java.security.Signature`, iOS uses `Security.framework`.

### KD-9: Public API Surface via explicit-api Mode
**Decision**: Enable Kotlin `explicitApi()` in all library modules. Only DSL builders and key types are `public`.
**Rationale**: Maven Central consumers should see a clean, intentional API. Internal implementation classes stay `internal`.

---

## API Design

### OkHttp Integration (Android)

```kotlin
// ─── Simple usage (zero-config, fail-open, all hosts) ───
val interceptor = certificateTransparencyInterceptor()

val client = OkHttpClient.Builder()
    .addNetworkInterceptor(interceptor)
    .build()

// ─── Advanced usage ───
val interceptor = certificateTransparencyInterceptor {
    // Host filtering (include by default if none specified)
    +"*.example.com"
    -"internal.example.com"

    // Policy preset
    policy = ChromeCtPolicy()  // or AppleCtPolicy() or custom CTPolicy impl

    // Failure handling
    failOnError = false  // default: false (fail-open)

    // Logging / reporting callback
    logger = { host, result ->
        Log.d("CT", "$host: $result")
    }

    // Log list configuration
    logListUrl = "https://custom-mirror.com/log_list.json"
    diskCache = AndroidDiskCache(context)
}

val client = OkHttpClient.Builder()
    .addNetworkInterceptor(interceptor)
    .build()
```

### Ktor Plugin (Multiplatform)

```kotlin
// ─── Android (OkHttp engine) ───
val client = HttpClient(OkHttp) {
    install(CertificateTransparency) {
        +"*.example.com"
        -"internal.example.com"
        policy = ChromeCtPolicy()
        failOnError = false
        logger = { host, result -> println("$host: $result") }
    }
}

// ─── iOS (Darwin engine) ───
val client = HttpClient(Darwin) {
    install(CertificateTransparency) {
        +"*.example.com"
        policy = AppleCtPolicy()
    }
}
```

### Core Policy API

```kotlin
// Built-in policies
val chrome = ChromeCtPolicy()
val apple = AppleCtPolicy()

// Custom policy
val custom = CTPolicy { certificate, sctResults ->
    // Must have at least 2 valid SCTs from distinct operators
    val validFromDistinct = sctResults
        .filter { it is SctVerificationResult.Valid }
        .distinctBy { it.logOperator }
    validFromDistinct.size >= 2
}
```

### Verification Result Hierarchy

```kotlin
sealed class VerificationResult {
    sealed class Success : VerificationResult() {
        data class Trusted(val scts: List<SctVerificationResult.Valid>) : Success()
        data object InsecureConnection : Success()
        data object DisabledForHost : Success()
    }
    sealed class Failure : VerificationResult() {
        data object NoScts : Failure()
        data class TooFewSctsTrusted(val found: Int, val required: Int) : Failure()
        data class TooFewDistinctOperators(val found: Int, val required: Int) : Failure()
        data class LogServersFailed(val errors: List<SctVerificationResult>) : Failure()
        data class UnknownError(val cause: Throwable) : Failure()
    }
}
```

---

## Dependency Graph

```
Phase 0 (Project Setup)
├── 0.1  Module directories
├── 0.2  settings.gradle.kts          ← 0.1
├── 0.3  Version catalog
├── 0.4  seal-core build.gradle.kts   ← 0.2, 0.3
├── 0.5  seal-android build.gradle    ← 0.2, 0.3, 0.4
├── 0.6  seal-ios build.gradle        ← 0.2, 0.3, 0.4
├── 0.7  seal-ktor build.gradle       ← 0.2, 0.3, 0.5, 0.6
├── 0.8  composeApp deps              ← 0.4, 0.5, 0.6, 0.7
├── 0.9  Publishing convention        ← 0.4..0.7
└── 0.10 .gitignore                   ← 0.1

Phase 1 (Core – commonMain)
├── 1.1  SCT data models              ← 0.4
├── 1.2  VerificationResult models    ← 1.1
├── 1.3  Log server models            ← 1.1
├── 1.4  Host pattern matching        ← 0.4
├── 1.5  SCT deserializer             ← 1.1
├── 1.6  ASN.1 DER parser             ← 0.4
├── 1.7  X.509 extension parser       ← 1.5, 1.6
├── 1.8  CryptoVerifier interface     ← 0.4
├── 1.9  SCT signature verifier       ← 1.1, 1.7, 1.8
├── 1.10 Log list V3 JSON parser      ← 0.4, 1.3
├── 1.11 Log list sig verification    ← 1.8, 1.10
├── 1.12 Log list service abstraction ← 1.10, 1.11
├── 1.13 Embedded baseline log list   ← 1.10
├── 1.14 Policy engine                ← 1.2, 1.3
├── 1.15 Core verification orch.      ← 1.5, 1.7, 1.9, 1.12, 1.14
└── 1.16 DSL builder base             ← 1.4, 1.14

Phase 2 (Core – Platform Actuals)
├── 2.1  Android CryptoVerifier       ← 1.8
├── 2.2  iOS CryptoVerifier           ← 1.8
├── 2.3  Android disk cache           ← 1.12
├── 2.4  iOS disk cache               ← 1.12
└── 2.5  Resource loading             ← 1.13

Phase 3 (seal-android)
├── 3.1  Conscrypt initializer        ← 0.5
├── 3.2  Conscrypt SCT extractor      ← 1.5, 3.1
├── 3.3  OCSP response parser         ← 1.6, 1.5
├── 3.4  Certificate chain cleaner    ← 1.7
├── 3.5  OkHttp network interceptor   ← 1.15, 1.16, 3.2, 3.3, 3.4
├── 3.6  OkHttp DSL builder           ← 1.16, 3.5
├── 3.7  X509TrustManager wrapper     ← 1.15, 3.4
└── 3.8  Android disk cache           ← 1.12

Phase 4 (seal-ios)
├── 4.1  SecTrust CT result checker   ← 0.6
├── 4.2  SecTrust cert extractor      ← 0.6
├── 4.3  iOS CT verifier              ← 1.15, 4.1, 4.2
├── 4.4  URLSession delegate helper   ← 1.16, 4.3
└── 4.5  iOS disk cache               ← 1.12

Phase 5 (seal-ktor)
├── 5.1  Ktor plugin definition       ← 1.16
├── 5.2  Android Ktor (OkHttp)        ← 3.5, 5.1
└── 5.3  iOS Ktor (Darwin)            ← 4.4, 5.1

Phase 6 (Demo + Docs)
├── 6.1  OkHttp demo screen           ← 3.5, 3.6
├── 6.2  Ktor demo screen             ← 5.1..5.3
├── 6.3  KDoc API docs                ← All phases
└── 6.4  README                       ← All phases

Phase 7 (Testing)
├── 7.1  SCT deserializer tests       ← 1.5
├── 7.2  ASN.1 parser tests           ← 1.6
├── 7.3  X.509 extraction tests       ← 1.7
├── 7.4  Log list parser tests        ← 1.10, 1.11
├── 7.5  Policy engine tests          ← 1.14
├── 7.6  Host matcher tests           ← 1.4
├── 7.7  SCT signature tests          ← 1.9, 2.1/2.2
├── 7.8  E2E verification tests       ← 1.15
├── 7.9  OkHttp interceptor tests     ← 3.5
└── 7.10 Mock log list tests          ← 1.12
```

### Critical Path

The longest dependency chain determining minimum time to first usable library:

```
0.1 → 0.2 → 0.4 → 1.1 → 1.5 → 1.7 → 1.9 → 1.15 → 3.5 → 3.6 (OkHttp ready)
                    └→ 1.6 ─────┘       └→ 1.8 (+ 2.1/2.2)
                    └→ 1.3 → 1.10 → 1.12 ┘
                    └→ 1.2 → 1.14 ────────┘
```

### Parallelization Opportunities

| Can be done in parallel                                      |
|--------------------------------------------------------------|
| 1.1 + 1.4 + 1.6 + 1.8 (all depend only on 0.4)             |
| 1.5 + 1.10 (after 1.1 / 1.3 respectively)                  |
| 2.1 + 2.2 (after 1.8, platform-independent)                 |
| 2.3 + 2.4 + 2.5 (after 1.12 / 1.13)                        |
| 3.1 + 3.3 + 3.4 (after respective deps, independent)        |
| 4.1 + 4.2 (after 0.6, independent)                          |
| 5.2 + 5.3 (after 5.1 + respective platform deps)            |
| 7.1..7.6 (can start as soon as respective code is done)      |

---

## Risk Register

| # | Risk | Impact | Likelihood | Mitigation |
|---|------|--------|------------|------------|
| R1 | Google changes V3 log list schema again | High (broken log list loading) | Medium | Lenient JSON parsing; embedded fallback; configurable URL |
| R2 | Conscrypt API changes / deprecation | Medium (Android SCT extraction breaks) | Low | Abstract behind interface; pin Conscrypt version; test against multiple versions |
| R3 | iOS Security.framework deprecations | Medium (cinterop breaks) | Low | Target stable API subset; test against Xcode 15/16/17 |
| R4 | Pure Kotlin ASN.1 parser edge cases | Medium (certain certs fail to parse) | Medium | Extensive test vectors from real-world certs; fuzz testing with random DER |
| R5 | Ktor engine API changes | Medium (plugin breaks) | Medium | Ktor 3.x is relatively stable; abstract engine hooks; test against Ktor point releases |
| R6 | Binary size impact from Conscrypt | Low (user complaint) | Low | Conscrypt is already commonly bundled; document footprint; explore lite/minimal configs |
| R7 | iOS TLS/OCSP SCTs unavailable for manual verification | Medium (incomplete verification on iOS) | Certain | Documented as platform limitation; hybrid approach with OS flag as proxy |
| R8 | Log list signature key rotation | Medium (signature verification fails) | Low | Support multiple known public keys; allow user-supplied key; warn on verification failure, don't crash |

---

## Summary Statistics

| Phase | Tasks | Low | Medium | High |
|-------|-------|-----|--------|------|
| 0: Project Restructuring   | 10 | 5 | 3 | 2 |
| 1: seal-core (commonMain)  | 16 | 4 | 7 | 5 |
| 2: seal-core (Actuals)     | 5  | 2 | 2 | 1 |
| 3: seal-android            | 8  | 3 | 2 | 3 |
| 4: seal-ios                | 5  | 1 | 2 | 2 |
| 5: seal-ktor               | 3  | 0 | 3 | 0 |
| 6: Demo + Docs             | 4  | 0 | 4 | 0 |
| 7: Testing                 | 10 | 1 | 7 | 2 |
| **Total**                  | **61** | **16** | **30** | **15** |

---

## Phase Index

| File | Phase | Description |
|------|-------|-------------|
| [phase-0-project-restructuring.md](phase-0-project-restructuring.md) | Phase 0 | Convert single-module template into multi-module library project with publishing support |
| [phase-1a-core-models-and-parsing.md](phase-1a-core-models-and-parsing.md) | Phase 1A | Foundational data models, host matching, SCT/ASN.1/X.509 parsers in commonMain |
| [phase-1b-core-crypto-verification-policy.md](phase-1b-core-crypto-verification-policy.md) | Phase 1B | Crypto interface, signature verification, log list management, policy engine, DSL builder |
| [phase-2-platform-actuals.md](phase-2-platform-actuals.md) | Phase 2 | Platform `actual` implementations for CryptoVerifier, disk cache, resource loading |
| [phase-3-seal-android.md](phase-3-seal-android.md) | Phase 3 | Android integration: Conscrypt, OkHttp interceptor, TrustManager, disk cache |
| [phase-4-seal-ios.md](phase-4-seal-ios.md) | Phase 4 | iOS integration: SecTrust, URLSession helper, disk cache |
| [phase-5-seal-ktor.md](phase-5-seal-ktor.md) | Phase 5 | Ktor client plugin with platform-specific bridging |
| [phase-6-demo-and-docs.md](phase-6-demo-and-docs.md) | Phase 6 | Demo app screens, KDoc documentation, README |
| [phase-7-testing.md](phase-7-testing.md) | Phase 7 | Comprehensive test suite across all components |
