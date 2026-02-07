# Seal — KMP Certificate Transparency Library: Implementation Plan

> **Version**: 1.0  
> **Created**: 2026-02-07  
> **Status**: Draft  
> **Reference**: [Kotlin Multiplatform CT Library Plan](Kotlin%20Multiplatform%20CT%20Library%20Plan.md)

---

## Table of Contents

1. [Overview](#overview)
2. [Module Dependency Diagram](#module-dependency-diagram)
3. [Key Design Decisions](#key-design-decisions)
4. [API Design](#api-design)
5. [Phase 0: Project Restructuring](#phase-0-project-restructuring)
6. [Phase 1: seal-core (commonMain)](#phase-1-seal-core-commonmain)
7. [Phase 2: seal-core (Platform Actuals)](#phase-2-seal-core-platform-actuals)
8. [Phase 3: seal-android](#phase-3-seal-android)
9. [Phase 4: seal-ios](#phase-4-seal-ios)
10. [Phase 5: seal-ktor](#phase-5-seal-ktor)
11. [Phase 6: Demo App + Documentation](#phase-6-demo-app--documentation)
12. [Phase 7: Testing](#phase-7-testing)
13. [Dependency Graph](#dependency-graph)
14. [Risk Register](#risk-register)

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

## Phase 0: Project Restructuring

Convert the single-module template into a multi-module library project with publishing support.

---

### Task 0.1: Create Module Directory Structure

**Description**: Create the directory skeleton for all four library modules with KMP source set directories.

**Files to create**:
```
seal-core/
  src/
    commonMain/kotlin/com/jermey/seal/core/
    commonTest/kotlin/com/jermey/seal/core/
    androidMain/kotlin/com/jermey/seal/core/
    iosMain/kotlin/com/jermey/seal/core/
seal-android/
  src/
    main/kotlin/com/jermey/seal/android/
    test/kotlin/com/jermey/seal/android/
seal-ios/
  src/
    iosMain/kotlin/com/jermey/seal/ios/
    iosArm64Main/kotlin/
    iosSimulatorArm64Main/kotlin/
seal-ktor/
  src/
    commonMain/kotlin/com/jermey/seal/ktor/
    androidMain/kotlin/com/jermey/seal/ktor/
    iosMain/kotlin/com/jermey/seal/ktor/
```

**Dependencies**: None  
**Acceptance Criteria**: Directories exist; Gradle sync recognizes all modules  
**Complexity**: Low

---

### Task 0.2: Update settings.gradle.kts

**Description**: Register all new modules in the root settings file.

**Files to modify**:
- `settings.gradle.kts`

**Changes**:
```kotlin
include(":composeApp")
include(":seal-core")
include(":seal-android")
include(":seal-ios")
include(":seal-ktor")
```

**Dependencies**: 0.1  
**Acceptance Criteria**: `./gradlew projects` lists all five modules  
**Complexity**: Low

---

### Task 0.3: Update Version Catalog with New Dependencies

**Description**: Add all required dependency versions and library aliases to `gradle/libs.versions.toml`.

**Files to modify**:
- `gradle/libs.versions.toml`

**New entries (versions)**:
```toml
kotlinx-serialization = "1.8.1"
kotlinx-datetime = "0.6.2"
kotlinx-io = "0.7.0"
kotlinx-coroutines = "1.10.2"
ktor = "3.1.3"
okhttp = "4.12.0"
conscrypt = "2.5.2"
```

**New entries (libraries)**:
```toml
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-io-core = { module = "org.jetbrains.kotlinx:kotlinx-io-core", version.ref = "kotlinx-io" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
conscrypt-android = { module = "org.conscrypt:conscrypt-android", version.ref = "conscrypt" }
```

**New entries (plugins)**:
```toml
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
maven-publish = { id = "maven-publish" }
```

**Dependencies**: None  
**Acceptance Criteria**: `./gradlew --refresh-dependencies` resolves all new aliases  
**Complexity**: Low

---

### Task 0.4: Create seal-core build.gradle.kts

**Description**: Configure the `:seal-core` module as a KMP library targeting Android + iOS with `kotlinx-serialization`, `kotlinx-datetime`, and `kotlinx-io` dependencies. Enable `explicitApi()`. Configure `maven-publish`.

**Files to create**:
- `seal-core/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
}

kotlin {
    explicitApi()

    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.jermey.seal.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

group = "io.github.jermey.seal"
version = "0.1.0"

publishing {
    // Maven Central configuration (GPG signing, POM metadata)
}
```

**Dependencies**: 0.2, 0.3  
**Acceptance Criteria**: `./gradlew :seal-core:assemble` compiles successfully, produces AAR and iOS framework  
**Complexity**: Medium

---

### Task 0.5: Create seal-android build.gradle.kts

**Description**: Configure `:seal-android` as an Android-only library module with Conscrypt and OkHttp dependencies. Depends on `:seal-core`.

**Files to create**:
- `seal-android/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    explicitApi()
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

    sourceSets {
        androidMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.conscrypt.android)
            implementation(libs.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "com.jermey.seal.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

group = "io.github.jermey.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.4  
**Acceptance Criteria**: `./gradlew :seal-android:assembleDebug` succeeds with Conscrypt and OkHttp on classpath  
**Complexity**: Medium

---

### Task 0.6: Create seal-ios build.gradle.kts

**Description**: Configure `:seal-ios` as a KMP library targeting iOS only. Depends on `:seal-core`.

**Files to create**:
- `seal-ios/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    explicitApi()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
        }
    }
}

group = "io.github.jermey.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.4  
**Acceptance Criteria**: `./gradlew :seal-ios:compileKotlinIosArm64` succeeds  
**Complexity**: Low

---

### Task 0.7: Create seal-ktor build.gradle.kts

**Description**: Configure `:seal-ktor` as a KMP library with Ktor client dependency. Platform source sets depend on `:seal-android` (Android) and `:seal-ios` (iOS).

**Files to create**:
- `seal-ktor/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    explicitApi()
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(project(":seal-android"))
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(project(":seal-ios"))
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.jermey.seal.ktor"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
}

group = "io.github.jermey.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.5, 0.6  
**Acceptance Criteria**: `./gradlew :seal-ktor:assemble` succeeds  
**Complexity**: Medium

---

### Task 0.8: Update composeApp to Depend on Library Modules

**Description**: Add library module dependencies to `composeApp/build.gradle.kts` so the demo app can exercise the library.

**Files to modify**:
- `composeApp/build.gradle.kts`

**Changes**: Add in `sourceSets`:
```kotlin
commonMain.dependencies {
    implementation(project(":seal-core"))
    implementation(project(":seal-ktor"))
}
androidMain.dependencies {
    implementation(project(":seal-android"))
}
iosMain.dependencies {
    implementation(project(":seal-ios"))
}
```

**Dependencies**: 0.4, 0.5, 0.6, 0.7  
**Acceptance Criteria**: `./gradlew :composeApp:assembleDebug` succeeds with library modules on classpath  
**Complexity**: Low

---

### Task 0.9: Configure Shared Publishing Convention Plugin

**Description**: Create a convention plugin (or `buildSrc`/included build) to share POM metadata, signing configuration, and Maven Central publishing setup across all library modules. Configure group ID `io.github.jermey.seal`.

**Files to create**:
- `build-logic/convention/build.gradle.kts`
- `build-logic/convention/src/main/kotlin/seal.publishing.gradle.kts`
- `build-logic/settings.gradle.kts`

Or alternatively, configure directly in root `build.gradle.kts` via `subprojects`/`allprojects`.

**Key bits**:
- Group ID: `io.github.jermey.seal`
- Artifact IDs: `seal-core`, `seal-android`, `seal-ios`, `seal-ktor`
- POM: name, description, URL, licenses (Apache 2.0), developers, SCM
- Signing: GPG key configuration via environment variables
- Repository: Sonatype OSSRH / Maven Central staging

**Dependencies**: 0.4, 0.5, 0.6, 0.7  
**Acceptance Criteria**: `./gradlew publishToMavenLocal` produces artifacts under `io/github/jermey/seal/` in `~/.m2`  
**Complexity**: High

---

### Task 0.10: Add .gitignore Entries for New Modules

**Description**: Ensure new module `build/` directories and generated files are properly gitignored.

**Files to modify**:
- `.gitignore` (root, create if missing)

**Dependencies**: 0.1  
**Acceptance Criteria**: `git status` does not show generated files  
**Complexity**: Low

---

## Phase 1: seal-core (commonMain)

Core data models, parsers, verification logic, and policy engine — all in pure Kotlin common code.

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

---

## Phase 2: seal-core (Platform Actuals)

Platform-specific implementations of `expect` declarations from Phase 1.

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

---

## Phase 3: seal-android

Android-specific integrations: Conscrypt, OkHttp, TrustManager.

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

---

## Phase 4: seal-ios

iOS-specific integrations: SecTrust, URLSession.

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

---

## Phase 5: seal-ktor

Ktor Client Plugin that provides a unified API across platforms.

---

### Task 5.1: Define Ktor CertificateTransparency Plugin

**Description**: Create the Ktor `HttpClientPlugin` (formerly `Feature`) registration with platform-agnostic configuration.

**Files to create**:
- `seal-ktor/src/commonMain/kotlin/com/jermey/seal/ktor/CertificateTransparencyPlugin.kt`

**Implementation**:
```kotlin
public val CertificateTransparency = createClientPlugin(
    "CertificateTransparency",
    ::CTConfigurationBuilder
) {
    val config = pluginConfig.build()
    // Platform-specific installation handled via expect/actual
    installPlatformCt(this, config)
}

internal expect fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
)
```

**Dependencies**: 1.16  
**Acceptance Criteria**: Plugin installs in Ktor client; configuration DSL works  
**Complexity**: Medium

---

### Task 5.2: Android Ktor Implementation (OkHttp Engine)

**Description**: Implement the `actual` platform CT installation for Android that bridges to the OkHttp interceptor.

**Files to create**:
- `seal-ktor/src/androidMain/kotlin/com/jermey/seal/ktor/PlatformCt.android.kt`

**Approach**: On Android with OkHttp engine, configure the engine's `OkHttpConfig` to add the `CertificateTransparencyInterceptor` as a network interceptor.

```kotlin
internal actual fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
) {
    // Hook into OkHttp engine configuration
    // Add CertificateTransparencyInterceptor to the underlying OkHttpClient
}
```

**Note**: This requires careful integration with Ktor's engine configuration mechanism. May need to use `on(SendingRequest)` or engine-specific hooks.

**Dependencies**: 3.5, 5.1  
**Acceptance Criteria**: `HttpClient(OkHttp) { install(CertificateTransparency) { ... } }` performs CT checks  
**Complexity**: Medium

---

### Task 5.3: iOS Ktor Implementation (Darwin Engine)

**Description**: Implement the `actual` platform CT installation for iOS that bridges to the SecTrust verification.

**Files to create**:
- `seal-ktor/src/iosMain/kotlin/com/jermey/seal/ktor/PlatformCt.ios.kt`

**Approach**: On iOS with Darwin engine, hook into the `handleChallenge` configuration to intercept server trust challenges and delegate to `IosCertificateTransparencyVerifier`.

```kotlin
internal actual fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
) {
    // Hook into Darwin engine's handleChallenge
    // Use IosCertificateTransparencyVerifier for server trust evaluation
}
```

**Dependencies**: 4.4, 5.1  
**Acceptance Criteria**: `HttpClient(Darwin) { install(CertificateTransparency) { ... } }` performs CT checks  
**Complexity**: Medium

---

## Phase 6: Demo App + Documentation

Update `composeApp` and create comprehensive documentation.

---

### Task 6.1: Update composeApp with OkHttp CT Demo

**Description**: Add a screen to the demo app demonstrating OkHttp with CT enforcement. Show a list of URLs being checked and their CT verification results.

**Files to modify**:
- `composeApp/build.gradle.kts` (add OkHttp + Ktor deps)
- `composeApp/src/androidMain/kotlin/com/jermey/seal/...` (OkHttp demo)
- `composeApp/src/commonMain/kotlin/com/jermey/seal/App.kt` (shared UI)

**Dependencies**: 3.5, 3.6  
**Acceptance Criteria**: Demo app builds and runs; shows CT verification results for sample HTTPS connections  
**Complexity**: Medium

---

### Task 6.2: Add Ktor CT Demo Screen

**Description**: Add a screen demonstrating Ktor client with CT plugin on both platforms.

**Files to modify/create**:
- `composeApp/src/commonMain/kotlin/com/jermey/seal/demo/KtorDemoScreen.kt`

**Dependencies**: 5.1, 5.2, 5.3  
**Acceptance Criteria**: Ktor plugin demo works on both Android and iOS  
**Complexity**: Medium

---

### Task 6.3: Write API Documentation (KDoc)

**Description**: Add comprehensive KDoc to all `public` API surfaces across all library modules.

**Scope**:
- All `public` classes, interfaces, functions, properties
- All `public` sealed class variants
- Builder DSL methods
- Module-level documentation (`package.md` or module docs)

**Dependencies**: All prior phases  
**Acceptance Criteria**: `./gradlew dokkaHtml` generates full API docs; zero undocumented public APIs  
**Complexity**: Medium

---

### Task 6.4: Write README with Usage Examples

**Description**: Update the root `README.md` with comprehensive library documentation.

**Files to modify**:
- `README.md`

**Sections**:
1. Overview / Badges
2. Installation (Gradle dependency coordinates)
3. Quick Start (minimal code)
4. OkHttp Integration (full example)
5. Ktor Integration (full example)
6. Configuration Reference
7. Custom Policies
8. iOS Specifics
9. FAQ / Troubleshooting
10. Contributing
11. License

**Dependencies**: All prior phases  
**Acceptance Criteria**: README covers all use cases; code examples compile  
**Complexity**: Medium

---

## Phase 7: Testing

Comprehensive test suite covering parsing, verification, policy, and integration.

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

*End of Implementation Plan*
