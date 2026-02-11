# Analysis: seal-ios Module → seal-core iosMain Migration

**Date:** 2026-02-11  
**Status:** Complete analysis  
**Related plan:** [jvm-wasmjs-targets-plan.md](../plans/jvm-wasmjs-targets-plan.md) (seal-ios was kept in that plan; this analysis covers its elimination)

---

## 1. Complete File Inventory — seal-ios/src/

| # | File Path | Package | Visibility |
|---|-----------|---------|------------|
| 1 | `iosMain/kotlin/com/jermey/seal/ios/IosCertificateTransparencyVerifier.kt` | `com.jermey.seal.ios` | `public` |
| 2 | `iosMain/kotlin/com/jermey/seal/ios/cache/IosDiskCache.kt` | `com.jermey.seal.ios.cache` | `public` |
| 3 | `iosMain/kotlin/com/jermey/seal/ios/urlsession/UrlSessionCtHelper.kt` | `com.jermey.seal.ios.urlsession` | `public` |
| 4 | `iosMain/kotlin/com/jermey/seal/ios/sectrust/SecTrustCertificateExtractor.kt` | `com.jermey.seal.ios.sectrust` | `internal` |
| 5 | `iosMain/kotlin/com/jermey/seal/ios/sectrust/SecTrustCtChecker.kt` | `com.jermey.seal.ios.sectrust` | `internal` |
| 6 | `iosMain/kotlin/com/jermey/seal/ios/.gitkeep` | — | — |

### File Details

#### 1. IosCertificateTransparencyVerifier.kt
- **Imports from seal-core:** `CTConfiguration`, `createCryptoVerifier`, `InMemoryLogListCache`, `LogListService`, `VerificationResult`, `CertificateTransparencyVerifier`
- **Imports from seal-ios:** `SecTrustCertificateExtractor`, `SecTrustCtChecker`
- **Platform imports:** `kotlinx.cinterop.ExperimentalForeignApi`, `platform.Security.SecTrustRef`
- **Description:** Main iOS CT verifier. Combines manual embedded-SCT verification (via seal-core's `CertificateTransparencyVerifier`) with OS-level CT checking (via `SecTrustCtChecker`). Uses `SecTrustCertificateExtractor` to extract DER certs from `SecTrustRef`.

#### 2. IosDiskCache.kt
- **Imports from seal-core:** `CachedLogList`, `IosDiskLogListCache`, `LogListCache`
- **Description:** Thin convenience wrapper around `IosDiskLogListCache.createDefault()` from seal-core. Implements `LogListCache` interface. Only 15 lines of actual code.

#### 3. UrlSessionCtHelper.kt
- **Imports from seal-core:** `CTConfiguration`, `VerificationResult`
- **Imports from seal-ios:** `IosCertificateTransparencyVerifier`
- **Platform imports:** All `platform.Foundation.NSURL*` types, `platform.Security.SecTrustRef`, `kotlinx.cinterop.*`, `kotlinx.coroutines.runBlocking`
- **Description:** URLSession authentication challenge handler for CT verification. Takes an `NSURLAuthenticationChallenge`, extracts `SecTrustRef`, runs the verifier, returns disposition + credential pair.

#### 4. SecTrustCertificateExtractor.kt
- **Imports:** Only `kotlinx.cinterop.*` and `platform.CoreFoundation.*`, `platform.Security.*`, `platform.posix.memcpy`
- **Description:** Extracts DER-encoded certificate byte arrays from a `SecTrustRef` using `SecTrustCopyCertificateChain` (iOS 15+). Pure iOS Security framework interop, no seal dependencies.

#### 5. SecTrustCtChecker.kt
- **Imports:** Only `kotlinx.cinterop.*`, `platform.CoreFoundation.*`, `platform.Security.*`
- **Description:** Reads the OS-level CT compliance result from `SecTrustCopyResult` dictionary (key: `TrustCertificateTransparency`). Pure iOS Security framework interop, no seal dependencies.

---

## 2. seal-ios Build Configuration

```kotlin
plugins {
    kotlinMultiplatform, dokka, "seal.publishing"
}

kotlin {
    explicitApi()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))                    // ← transitive API dep
            implementation(libs.kotlinx.coroutines.core)  // ← already in seal-core's commonMain
        }
    }
}
```

**Key observations:**
- `api(project(":seal-core"))` — seal-ios exposes seal-core types to consumers
- `kotlinx.coroutines.core` — redundant, already provided by seal-core's `commonMain`
- No additional external dependencies beyond seal-core
- Targets: `iosArm64` + `iosSimulatorArm64` (same as seal-core)

---

## 3. Non-iOS API Check

**Result: CLEAN.** No files in seal-ios reference `android.*`, `java.*`, or any non-iOS platform APIs. All platform imports are exclusively:
- `platform.Security.*` (SecTrust, SecCertificate)
- `platform.CoreFoundation.*` (CFArray, CFData, CFDictionary, CFString, CFBoolean)
- `platform.Foundation.*` (NSURLSession types, NSURLCredential)
- `platform.posix.memcpy`
- `kotlinx.cinterop.*`

---

## 4. Package Mapping: seal-ios → seal-core iosMain

| Current Package (seal-ios) | Target Package (seal-core iosMain) | Rationale |
|---|---|---|
| `com.jermey.seal.ios` | `com.jermey.seal.ios` | **Keep as-is.** Public API package — changing would break downstream consumers (seal-ktor, composeApp). Consistent with `com.jermey.seal.jvm.*` pattern in the JVM plan. |
| `com.jermey.seal.ios.cache` | `com.jermey.seal.ios.cache` | **Keep as-is.** Public convenience class. |
| `com.jermey.seal.ios.urlsession` | `com.jermey.seal.ios.urlsession` | **Keep as-is.** Public URLSession integration API. |
| `com.jermey.seal.ios.sectrust` | `com.jermey.seal.ios.sectrust` | **Keep as-is.** Internal classes. Package stays for organizational clarity. |

**Rationale for keeping `com.jermey.seal.ios.*` packages:**
The JVM plan renames `com.jermey.seal.android.*` → `com.jermey.seal.jvm.*` for the code moving from `seal-android` to `seal-core/src/jvmSharedMain/`. The iOS code has no such rename need — `com.jermey.seal.ios` is already the correct platform-specific package name. Moving files from `seal-ios` module into `seal-core`'s `iosMain` source set doesn't require a package change.

---

## 5. Dependency Conflict Analysis

| Dependency | seal-ios | seal-core iosMain | Conflict? |
|---|---|---|---|
| `kotlinx-coroutines-core` | `implementation` | `implementation` (commonMain) | No — already in seal-core, seal-ios dep is redundant |
| `seal-core` | `api` | N/A (self) | No — code moves into seal-core itself |

**No dependency conflicts.** seal-ios has zero dependencies beyond seal-core and coroutines (both already in seal-core).

---

## 6. External Consumers of seal-ios

| Consumer | File | Import | Impact |
|---|---|---|---|
| **seal-ktor** | `build.gradle.kts` (line ~36) | `implementation(project(":seal-ios"))` | Remove dependency; import path unchanged |
| **seal-ktor** | `src/iosMain/.../PlatformCt.ios.kt` (line 7) | `import com.jermey.seal.ios.IosCertificateTransparencyVerifier` | **No change needed** — same package in new location |
| **composeApp** | `build.gradle.kts` (line ~69) | `implementation(project(":seal-ios"))` | Remove dependency (seal-core already included) |
| **root** | `build.gradle.kts` (line ~16) | `dokka(project(":seal-ios"))` | Remove |
| **root** | `settings.gradle.kts` (line ~39) | `include(":seal-ios")` | Remove |

**Key insight:** Since packages stay the same, no Kotlin source file changes are needed in consumers — only Gradle build files need updating.

---

## 7. Target Directory Structure in seal-core/src/iosMain/

### Current seal-core iosMain:
```
seal-core/src/iosMain/kotlin/com/jermey/seal/core/
├── .gitkeep
├── crypto/CryptoProvider.ios.kt
└── loglist/
    ├── DiskLogListCache.ios.kt
    └── ResourceLoader.ios.kt
```

### After merge (new files marked with ✚):
```
seal-core/src/iosMain/kotlin/com/jermey/seal/
├── core/
│   ├── .gitkeep
│   ├── crypto/CryptoProvider.ios.kt
│   └── loglist/
│       ├── DiskLogListCache.ios.kt
│       └── ResourceLoader.ios.kt
└── ios/                                              ✚ NEW SUBTREE
    ├── IosCertificateTransparencyVerifier.kt         ✚
    ├── cache/
    │   └── IosDiskCache.kt                           ✚
    ├── urlsession/
    │   └── UrlSessionCtHelper.kt                     ✚
    └── sectrust/
        ├── SecTrustCertificateExtractor.kt           ✚
        └── SecTrustCtChecker.kt                      ✚
```

Note: The new `ios/` package sits alongside the existing `core/` package under `com/jermey/seal/`, maintaining the original package hierarchy.

---

## 8. Move Strategy

### Step-by-step:

1. **Create directory** `seal-core/src/iosMain/kotlin/com/jermey/seal/ios/` (and subdirs: `cache/`, `urlsession/`, `sectrust/`)

2. **Copy 5 files** from `seal-ios/src/iosMain/kotlin/com/jermey/seal/ios/` to `seal-core/src/iosMain/kotlin/com/jermey/seal/ios/` — **no package or import changes needed**

3. **Update Gradle build files:**
   - `seal-ktor/build.gradle.kts`: Remove `implementation(project(":seal-ios"))` from iosMain dependencies
   - `composeApp/build.gradle.kts`: Remove `implementation(project(":seal-ios"))` from iosMain dependencies  
   - `build.gradle.kts` (root): Remove `dokka(project(":seal-ios"))`
   - `settings.gradle.kts`: Remove `include(":seal-ios")`

4. **Delete** `seal-ios/` module directory entirely

5. **Verify** `./gradlew :seal-core:compileKotlinIosArm64 :seal-core:compileKotlinIosSimulatorArm64 :seal-ktor:compileKotlinIosArm64 :composeApp:compileKotlinIosArm64`

### What does NOT need to change:
- Package declarations in any Kotlin file
- Import statements in any Kotlin file (in seal-ios or consumers)
- seal-core's `build.gradle.kts` (iosArm64/iosSimulatorArm64 targets already configured, coroutines already in commonMain)

---

## 9. AndroidDiskCache.kt (seal-android) — Full Source

For completeness, as requested:

```kotlin
package com.jermey.seal.android.cache

import android.content.Context
import com.jermey.seal.core.loglist.AndroidDiskLogListCache
import com.jermey.seal.core.loglist.CachedLogList
import com.jermey.seal.core.loglist.LogListCache

public class AndroidDiskCache(context: Context) : LogListCache {
    private val delegate: AndroidDiskLogListCache =
        AndroidDiskLogListCache(context.cacheDir)

    override suspend fun get(): CachedLogList? = delegate.get()
    override suspend fun put(logList: CachedLogList) { delegate.put(logList) }
}
```

**Location:** `seal-android/src/androidMain/kotlin/com/jermey/seal/android/cache/AndroidDiskCache.kt`  
**Dependencies:** `android.content.Context` (Android-only), `AndroidDiskLogListCache` from seal-core  
**Move target:** `seal-core/src/androidMain/kotlin/com/jermey/seal/android/cache/AndroidDiskCache.kt`  
**Package change needed:** None (`com.jermey.seal.android.cache` stays, same pattern as iOS)

**Note from JVM plan:** The JVM plan says "only `AndroidDiskCache.kt` remains" in seal-android. If we're now eliminating seal-android *entirely*, this file moves to `seal-core/src/androidMain/` with the same package. seal-core already has `androidMain` source set with `android.content.Context` available.

---

## 10. Risk Assessment

| Risk | Severity | Notes |
|------|----------|-------|
| Package collision in seal-core iosMain | **None** | New `com.jermey.seal.ios.*` packages don't overlap with existing `com.jermey.seal.core.*` |
| Missing dependencies after move | **None** | seal-ios's only deps (coroutines, seal-core) are already in seal-core's commonMain |
| Consumer breakage | **None** | Packages unchanged; only Gradle dependency lines need removal |
| `explicitApi()` mode mismatch | **None** | Both seal-ios and seal-core use `explicitApi()` — visibility modifiers already present |
| Dokka/publishing impact | **Low** | seal-ios dokka config moves into seal-core's; publishing config may need adjustment |

---

## Summary

**This is one of the cleanest possible module eliminations.** seal-ios has:
- Zero external dependencies beyond seal-core
- Zero package rename requirements
- Zero import changes in any source file
- Only 5 Kotlin files to move
- Only 4 Gradle files to update (remove dependency/include lines)

The entire migration is a file copy + Gradle cleanup.
