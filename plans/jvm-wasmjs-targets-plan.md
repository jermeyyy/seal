# Implementation Plan: JVM Desktop & wasmJs Browser Targets + Module Consolidation

**Date:** 2026-02-11
**Version:** 0.1.0 → 0.2.0
**Status:** Ready for Implementation
**Breaking Changes:** Yes (accepted — module consolidation, package changes)

---

## Overview

Add JVM desktop and wasmJs browser targets to the Seal Certificate Transparency library while consolidating all platform modules (`seal-android`, `seal-ios`) into `seal-core`. This creates a clean single-library architecture where KMP Maven publishing automatically delivers the right platform code to each consumer.

---

## Architecture Decisions

1. **Eliminate `seal-android` module** — all Android code moves to `seal-core/src/androidMain/`
2. **Eliminate `seal-ios` module** — all iOS code moves to `seal-core/src/iosMain/` (zero renames needed, packages stay `com.jermey.seal.ios.*`)
3. **Keep `seal-ktor` as separate optional module** — Ktor is an optional dependency, consumers who don't use Ktor shouldn't need it
4. **Intermediate source set `jvmSharedMain`** in `seal-core` — shared between `androidMain` and `jvmMain`, contains all Conscrypt/OkHttp/TrustManager code
5. **9 files from seal-android** move to `seal-core/src/jvmSharedMain/` with package rename `com.jermey.seal.android.*` → `com.jermey.seal.jvm.*`
6. **AndroidDiskCache** moves to `seal-core/src/androidMain/` — keeps `android.content.Context` parameter in config DSL
7. **5 files from seal-ios** move to `seal-core/src/iosMain/` — zero package renames, zero import changes
8. **`OcspResponseParser`** moves to `seal-core/src/commonMain/` under `com.jermey.seal.core.parser` — prepares for future cross-platform OCSP support
9. **`android.util.Log` replaced** with `java.util.logging.Logger` in 2 files (ConscryptSctExtractor, ConscryptCtSocketFactory)
10. **wasmJs target** — audit-only, browser handles CT natively, stub CryptoVerifier
11. **Conscrypt-OpenJDK required** on JVM desktop for full SCT extraction parity with Android
12. **Smart Maven packaging** — KMP metadata ensures consumers get correct platform artifacts automatically:
    - Android consumers: `seal-core-android` artifact → commonMain + jvmSharedMain + androidMain
    - iOS consumers: `seal-core-iosarm64`/`seal-core-iossimulatorarm64` → commonMain + iosMain
    - JVM consumers: `seal-core-jvm` artifact → commonMain + jvmSharedMain + jvmMain
    - wasmJs consumers: `seal-core-wasm-js` artifact → commonMain + wasmJsMain

---

## Module Structure

### Current (before)

| Module | Targets | Purpose |
|--------|---------|---------|
| seal-core | Android, iOS | Common CT logic, ASN.1/SCT parsing, verification engine |
| seal-android | Android | Conscrypt integration, OkHttp interceptor, TrustManager |
| seal-ios | iOS | SecTrust evaluation, URLSession helpers |
| seal-ktor | Android, iOS | Ktor HttpClient plugin (optional) |
| composeApp | Android, iOS | Demo app |
| androidApp | Android | Android host for demo |

### Target (after)

| Module | Targets | Purpose |
|--------|---------|---------|
| **seal-core** | Android, iOS, **JVM**, **wasmJs** | **All CT logic**: common + Android (Conscrypt/OkHttp) + iOS (SecTrust/URLSession) + JVM (Conscrypt-OpenJDK/OkHttp) + wasmJs (audit-only) |
| ~~seal-android~~ | ~~removed~~ | Merged into seal-core |
| ~~seal-ios~~ | ~~removed~~ | Merged into seal-core |
| **seal-ktor** | Android, iOS, **JVM**, **wasmJs** | Ktor HttpClient plugin (optional) |
| **composeApp** | Android, iOS, **JVM Desktop**, **wasmJs** | Demo app |
| androidApp | Android | Android host for demo |

---

## seal-core Source Set Hierarchy

```
commonMain
├── jvmSharedMain ← shared JVM code (OkHttp interceptor, Conscrypt, TrustManager, chain cleaner)
│   ├── androidMain ← Android-specific (CryptoProvider, ResourceLoader, DiskLogListCache, AndroidDiskCache)
│   └── jvmMain ← JVM desktop-specific (CryptoProvider, ResourceLoader, DiskLogListCache)
├── iosMain ← iOS-specific (SecTrust, URLSession, IosCertificateTransparencyVerifier, CryptoProvider, ResourceLoader, DiskLogListCache, IosDiskCache)
└── wasmJsMain ← Browser-specific (stub CryptoVerifier, stub ResourceLoader)
```

### Dependencies by Source Set

| Source Set | Key Dependencies |
|-----------|-----------------|
| commonMain | kotlinx-serialization, kotlinx-datetime, kotlinx-io, kotlinx-coroutines-core |
| jvmSharedMain | okhttp |
| androidMain | conscrypt-android |
| jvmMain | conscrypt-openjdk |
| iosMain | (no external deps — uses Apple Security/Foundation frameworks) |
| wasmJsMain | (no external deps) |

---

## Files Moving from seal-android to seal-core

### To jvmSharedMain (package rename: `com.jermey.seal.android.*` → `com.jermey.seal.jvm.*`)

| File | New Location in seal-core | Pre-move Action |
|------|--------------------------|-----------------|
| `chain/CertificateChainCleaner.kt` | `jvmSharedMain/.../jvm/chain/` | None |
| `conscrypt/ConscryptSctExtractor.kt` | `jvmSharedMain/.../jvm/conscrypt/` | Replace `android.util.Log` → `java.util.logging.Logger` |
| `ConscryptInitializer.kt` | `jvmSharedMain/.../jvm/` | None |
| `trust/CTTrustManagerFactory.kt` | `jvmSharedMain/.../jvm/trust/` | None |
| `trust/CTTrustManager.kt` | `jvmSharedMain/.../jvm/trust/` | None |
| `okhttp/CertificateTransparencyDsl.kt` | `jvmSharedMain/.../jvm/okhttp/` | None |
| `okhttp/ConscryptCtSocketFactory.kt` | `jvmSharedMain/.../jvm/okhttp/` | Replace `android.util.Log` → `java.util.logging.Logger` |
| `okhttp/CertificateTransparencyInterceptor.kt` | `jvmSharedMain/.../jvm/okhttp/` | None |

### To commonMain (package rename to `com.jermey.seal.core.parser`)

| File | New Location in seal-core | Pre-move Action |
|------|--------------------------|-----------------|
| `conscrypt/OcspResponseParser.kt` | `commonMain/.../core/parser/` | Package rename (no JVM-specific APIs, prepares for future OCSP support) |

### To androidMain (no package rename — stays `com.jermey.seal.android.*`)

| File | New Location in seal-core | Pre-move Action |
|------|--------------------------|-----------------|
| `cache/AndroidDiskCache.kt` | `androidMain/.../android/cache/` | None |

### Test file

| File | New Location | Pre-move Action |
|------|-------------|-----------------|
| `CertificateTransparencyInterceptorTest.kt` | seal-core jvmSharedTest or jvmTest | Package rename to match |

---

## Files Moving from seal-ios to seal-core iosMain

**Zero package renames needed.** All files keep their `com.jermey.seal.ios.*` packages.

| File | Current Package | New Location in seal-core | Notes |
|------|----------------|--------------------------|-------|
| `IosCertificateTransparencyVerifier.kt` | `com.jermey.seal.ios` | `iosMain/.../ios/` | public, main iOS verifier |
| `cache/IosDiskCache.kt` | `com.jermey.seal.ios.cache` | `iosMain/.../ios/cache/` | public, wraps IosDiskLogListCache |
| `urlsession/UrlSessionCtHelper.kt` | `com.jermey.seal.ios.urlsession` | `iosMain/.../ios/urlsession/` | public, URLSession integration |
| `sectrust/SecTrustCertificateExtractor.kt` | `com.jermey.seal.ios.sectrust` | `iosMain/.../ios/sectrust/` | internal |
| `sectrust/SecTrustCtChecker.kt` | `com.jermey.seal.ios.sectrust` | `iosMain/.../ios/sectrust/` | internal |

---

## Expect Declarations Needing New Actuals

| Declaration | Location | jvmMain Actual | wasmJsMain Actual |
|------------|----------|----------------|-------------------|
| `createCryptoVerifier()` | `commonMain/crypto/CryptoProvider.kt` | Reuse `JvmCryptoVerifier` (java.security.*) | Stub throwing `UnsupportedOperationException` |
| `ResourceLoader` | `commonMain/loglist/ResourceLoader.kt` | Classpath loading (copy from androidMain) | Stub |

---

## Plan Phases

### Phase 1: seal-core — Add JVM + wasmJs Targets and jvmSharedMain Source Set

**Goal:** Establish the multi-target foundation in seal-core before moving any code.

**Tasks:**

#### 1.1 Update seal-core/build.gradle.kts
- Add `jvm()` target
- Add `wasmJs { browser() }` target
- Configure `jvmSharedMain` intermediate source set depending on `commonMain`
- Configure `androidMain` and `jvmMain` to depend on `jvmSharedMain`
- Add OkHttp dependency to `jvmSharedMain`
- Add `conscrypt-android` dependency to `androidMain`
- Add `conscrypt-openjdk` dependency to `jvmMain`
- **Files:** `seal-core/build.gradle.kts`
- **Depends on:** nothing

#### 1.2 Update gradle/libs.versions.toml
- Add `conscrypt-openjdk` library entry (same version as conscrypt-android)
- Add `ktor-client-js` library entry
- Add `kotlinx-coroutines-swing` library entry
- **Files:** `gradle/libs.versions.toml`
- **Depends on:** nothing

#### 1.3 Create JVM CryptoProvider actual
- Create `seal-core/src/jvmMain/kotlin/com/jermey/seal/core/crypto/CryptoProvider.jvm.kt`
- Implement `actual fun createCryptoVerifier()` returning `JvmCryptoVerifier` (copy from androidMain — uses only `java.security.*`)
- **Files:** 1 new file
- **Depends on:** 1.1

#### 1.4 Create JVM ResourceLoader actual
- Create `seal-core/src/jvmMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.jvm.kt`
- Implement using classpath loading (copy pattern from androidMain)
- **Files:** 1 new file
- **Depends on:** 1.1

#### 1.5 Create JVM DiskLogListCache
- Create `seal-core/src/jvmMain/kotlin/com/jermey/seal/core/loglist/JvmDiskLogListCache.kt`
- File-based cache using `java.io.File` (same as existing `AndroidDiskLogListCache`)
- **Files:** 1 new file
- **Depends on:** 1.1

#### 1.6 Create wasmJs CryptoProvider actual
- Create `seal-core/src/wasmJsMain/kotlin/com/jermey/seal/core/crypto/CryptoProvider.wasmJs.kt`
- Stub implementation throwing `UnsupportedOperationException` (browser handles CT natively)
- **Files:** 1 new file
- **Depends on:** 1.1

#### 1.7 Create wasmJs ResourceLoader actual
- Create `seal-core/src/wasmJsMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.wasmJs.kt`
- Stub implementation
- **Files:** 1 new file
- **Depends on:** 1.1

#### 1.8 Validate JVM target
- Run existing commonTest on JVM: `./gradlew :seal-core:jvmTest`
- Verify all common tests pass on JVM runtime
- **Depends on:** 1.3, 1.4

**Acceptance criteria:** `./gradlew :seal-core:jvmTest` passes. `./gradlew :seal-core:compileKotlinWasmJs` succeeds.

---

### Phase 2: Consolidate seal-android into seal-core

**Goal:** Move all seal-android code into seal-core's source sets and delete the module.

**Tasks:**

#### 2.1 Move OcspResponseParser to commonMain
- Move `seal-android/.../conscrypt/OcspResponseParser.kt` → `seal-core/src/commonMain/kotlin/com/jermey/seal/core/parser/OcspResponseParser.kt`
- Change package from `com.jermey.seal.android.conscrypt` to `com.jermey.seal.core.parser`
- **Files:** 1 new file in seal-core, 1 deleted from seal-android
- **Depends on:** Phase 1

#### 2.2 Move shared JVM files to jvmSharedMain
- Move 8 files from seal-android to `seal-core/src/jvmSharedMain/kotlin/com/jermey/seal/jvm/`:
  - `chain/CertificateChainCleaner.kt` (package rename)
  - `conscrypt/ConscryptSctExtractor.kt` (package rename + replace `android.util.Log` → `java.util.logging.Logger`)
  - `ConscryptInitializer.kt` (package rename)
  - `trust/CTTrustManagerFactory.kt` (package rename)
  - `trust/CTTrustManager.kt` (package rename)
  - `okhttp/CertificateTransparencyDsl.kt` (package rename)
  - `okhttp/ConscryptCtSocketFactory.kt` (package rename + replace `android.util.Log` → `java.util.logging.Logger`)
  - `okhttp/CertificateTransparencyInterceptor.kt` (package rename)
- All packages change: `com.jermey.seal.android.*` → `com.jermey.seal.jvm.*`
- **Files:** 8 new files in seal-core, 8 deleted from seal-android
- **Depends on:** 2.1 (OcspResponseParser must move first since ConscryptSctExtractor references it)

#### 2.3 Move AndroidDiskCache to seal-core androidMain
- Move `seal-android/.../cache/AndroidDiskCache.kt` → `seal-core/src/androidMain/kotlin/com/jermey/seal/android/cache/AndroidDiskCache.kt`
- Keep package `com.jermey.seal.android.cache` (Android-specific convenience)
- Update imports to reference `com.jermey.seal.jvm.*` for any shared types
- **Files:** 1 new file in seal-core, 1 deleted from seal-android
- **Depends on:** 2.2

#### 2.4 Move interceptor test
- Move `seal-android/.../CertificateTransparencyInterceptorTest.kt` to seal-core test source set
- Update package to `com.jermey.seal.jvm`
- **Files:** 1 new test file in seal-core, 1 deleted from seal-android
- **Depends on:** 2.2

#### 2.5 Update seal-android build.gradle.kts dependencies in consuming modules
- Update `seal-ktor` — replace `project(":seal-android")` dependency with `project(":seal-core")` for androidMain
- Update `composeApp` — replace any `project(":seal-android")` dependency
- **Files:** `seal-ktor/build.gradle.kts`, `composeApp/build.gradle.kts`
- **Depends on:** 2.2

#### 2.6 Delete seal-android module
- Remove `seal-android/` directory
- Remove `include(":seal-android")` from `settings.gradle.kts`
- Remove seal-android from root `build.gradle.kts` if referenced
- **Files:** delete `seal-android/`, update `settings.gradle.kts`, `build.gradle.kts`
- **Depends on:** 2.3, 2.4, 2.5

#### 2.7 Verify compilation
- `./gradlew :seal-core:compileDebugKotlinAndroid :seal-core:compileKotlinJvm`
- Run interceptor test from new location
- **Depends on:** 2.6

**Acceptance criteria:** seal-android module deleted. seal-core compiles for Android and JVM. All tests pass.

---

### Phase 3: Consolidate seal-ios into seal-core

**Goal:** Move all seal-ios code into seal-core's iosMain and delete the module.

**Tasks:**

#### 3.1 Move iOS files to seal-core iosMain
- Move 5 files from `seal-ios/src/iosMain/` to `seal-core/src/iosMain/`:
  - `IosCertificateTransparencyVerifier.kt` (no package change — `com.jermey.seal.ios`)
  - `cache/IosDiskCache.kt` (no package change — `com.jermey.seal.ios.cache`)
  - `urlsession/UrlSessionCtHelper.kt` (no package change — `com.jermey.seal.ios.urlsession`)
  - `sectrust/SecTrustCertificateExtractor.kt` (no package change — `com.jermey.seal.ios.sectrust`)
  - `sectrust/SecTrustCtChecker.kt` (no package change — `com.jermey.seal.ios.sectrust`)
- Zero import changes needed in moved files or consumers
- **Files:** 5 new files in seal-core, 5 deleted from seal-ios
- **Depends on:** Phase 1

#### 3.2 Update consuming modules
- Update `seal-ktor` — replace `project(":seal-ios")` dependency with `project(":seal-core")` for iosMain
- Update `composeApp` — replace any `project(":seal-ios")` dependency
- **Files:** `seal-ktor/build.gradle.kts`, `composeApp/build.gradle.kts`
- **Depends on:** 3.1

#### 3.3 Delete seal-ios module
- Remove `seal-ios/` directory
- Remove `include(":seal-ios")` from `settings.gradle.kts`
- Remove seal-ios from root `build.gradle.kts` if referenced
- **Files:** delete `seal-ios/`, update `settings.gradle.kts`, `build.gradle.kts`
- **Depends on:** 3.2

#### 3.4 Verify compilation
- `./gradlew :seal-core:compileKotlinIosArm64 :seal-core:compileKotlinIosSimulatorArm64`
- **Depends on:** 3.3

**Acceptance criteria:** seal-ios module deleted. seal-core compiles for all iOS targets. No import changes needed in consumers.

---

### Phase 4: seal-ktor — Add JVM + wasmJs Targets

**Goal:** Extend the Ktor plugin to support JVM desktop and wasmJs.

**Tasks:**

#### 4.1 Update seal-ktor/build.gradle.kts
- Add `jvm()` and `wasmJs { browser() }` targets
- Add `ktor-client-okhttp` dependency for jvmMain
- Add `ktor-client-js` dependency for wasmJsMain
- Remove old `project(":seal-android")` and `project(":seal-ios")` deps (done in Phase 2/3)
- **Files:** `seal-ktor/build.gradle.kts`
- **Depends on:** Phase 2, Phase 3

#### 4.2 Create jvmMain Ktor integration
- Create `installPlatformCt()` actual (no-op, same pattern as Android/iOS)
- Create `HttpClientConfig<OkHttpConfig>.certificateTransparency()` extension that configures OkHttp with seal-core's shared Conscrypt/interceptor code
- **Files:** 1-2 new files in `seal-ktor/src/jvmMain/`
- **Depends on:** 4.1

#### 4.3 Create wasmJsMain Ktor integration
- Create `installPlatformCt()` actual (no-op — browser handles CT natively)
- **Files:** 1 new file in `seal-ktor/src/wasmJsMain/`
- **Depends on:** 4.1

#### 4.4 Verify compilation
- `./gradlew :seal-ktor:compileKotlinJvm :seal-ktor:compileKotlinWasmJs`
- **Depends on:** 4.2, 4.3

**Acceptance criteria:** seal-ktor compiles for all 4 target families (Android, iOS, JVM, wasmJs).

---

### Phase 5: Demo App — Desktop (JVM)

**Goal:** Run the Compose Multiplatform demo app on desktop with full CT verification.

**Tasks:**

#### 5.1 Update composeApp/build.gradle.kts
- Add `jvm("desktop")` target
- Add desktop dependencies: seal-core, seal-ktor, kotlinx-coroutines-swing, compose.desktop.currentOs
- **Files:** `composeApp/build.gradle.kts`
- **Depends on:** Phase 4

#### 5.2 Create desktop entry point
- Create `composeApp/src/desktopMain/kotlin/com/jermey/seal/main.kt` with Compose Desktop `Window` and `application {}` block
- **Files:** 1 new file
- **Depends on:** 5.1

#### 5.3 Create desktop platform actuals
- `getPlatform()` → "JVM Desktop"
- `isAndroid` → false
- `createCtHttpClient()` → OkHttp client with CT via seal-core's shared Conscrypt code
- `CtVerificationRepository` → full verification support
- **Files:** 3-4 new files in desktopMain
- **Depends on:** 5.1

#### 5.4 Verify demo dependencies
- Check FlowMVI, Koin, Quo Vadis support on JVM desktop
- Implement workarounds/stubs for any unsupported library
- **Depends on:** 5.3

#### 5.5 Build and test
- `./gradlew :composeApp:run` (desktop)
- **Depends on:** 5.4

**Acceptance criteria:** Desktop demo launches, shows CT verification results.

---

### Phase 6: Demo App — Web (wasmJs)

**Goal:** Run the Compose Multiplatform demo app in a browser with audit-only CT info.

**Tasks:**

#### 6.1 Update composeApp/build.gradle.kts
- Add `wasmJs { browser { commonWebpackConfig { outputFileName = "seal-demo.js" } } }` target
- Add wasmJs dependencies: seal-core, seal-ktor, ktor-client-js
- **Files:** `composeApp/build.gradle.kts`
- **Depends on:** Phase 4

#### 6.2 Create browser entry point
- Create `composeApp/src/wasmJsMain/kotlin/com/jermey/seal/main.kt` with `fun main()` calling Compose `CanvasBasedWindow` or `ComposeViewport`
- **Files:** 1 new file
- **Depends on:** 6.1

#### 6.3 Create wasmJs platform actuals
- `getPlatform()` → "Web Browser"
- `isAndroid` → false
- `createCtHttpClient()` → Ktor Js engine (no CT plugin, browser native)
- `CtVerificationRepository` → audit-only, reports browser CT status
- **Files:** 3-4 new files in wasmJsMain
- **Depends on:** 6.1

#### 6.4 Create index.html
- Create `composeApp/src/wasmJsMain/resources/index.html` web entry page
- **Files:** 1 new file
- **Depends on:** 6.1

#### 6.5 Verify demo dependencies
- Check FlowMVI, Koin, Quo Vadis support on wasmJs
- Implement workarounds/simplified alternatives for unsupported libraries
- **Depends on:** 6.3

#### 6.6 Build and test
- `./gradlew :composeApp:wasmJsBrowserDevelopmentRun`
- **Depends on:** 6.5

**Acceptance criteria:** Web demo loads in browser, shows CT audit results.

---

### Phase 7: Documentation, Publishing & Cleanup

**Goal:** Update all documentation and publishing configuration for the new architecture.

**Tasks:**

#### 7.1 Update settings.gradle.kts
- Remove `include(":seal-android")` (done in Phase 2)
- Remove `include(":seal-ios")` (done in Phase 3)
- Verify remaining modules compile
- **Files:** `settings.gradle.kts`
- **Depends on:** All previous phases

#### 7.2 Update README.md
- Update supported platforms (add JVM Desktop, wasmJs)
- Remove references to seal-android and seal-ios modules
- Update installation instructions (single `seal-core` dependency for all platforms)
- Add JVM Desktop usage examples
- Document Maven packaging: explain that KMP metadata delivers correct platform code
- **Files:** `README.md`
- **Depends on:** All previous phases

#### 7.3 Update docs website
- Add JVM Desktop guide page
- Add Web/wasmJs guide page
- Update platform compatibility matrix
- Remove seal-android/seal-ios specific guides (replace with unified seal-core guide)
- **Files:** `docs/site/src/pages/`
- **Depends on:** All previous phases

#### 7.4 Configure publishing for new targets
- Verify `seal.publishing` convention plugin handles JVM and wasmJs artifacts
- Ensure seal-core publishes: `-android`, `-iosarm64`, `-iossimulatorarm64`, `-jvm`, `-wasm-js` artifacts
- Ensure seal-ktor publishes same target set
- **Files:** `build-logic/convention/src/main/kotlin/seal.publishing.gradle.kts`
- **Depends on:** All previous phases

#### 7.5 Update CHANGELOG.md
- Document: new JVM + wasmJs targets
- Document: seal-android + seal-ios merged into seal-core (breaking change)
- Document: package renames (`com.jermey.seal.android.*` → `com.jermey.seal.jvm.*` for shared code)
- Document: OcspResponseParser moved to `com.jermey.seal.core.parser`
- Migration guide for existing consumers
- **Files:** `CHANGELOG.md`
- **Depends on:** All previous phases

#### 7.6 Clean up dead code
- Evaluate removing `ResourceLoader` expect/actual (unused dead code) or keep with all actuals now implemented
- Remove any orphaned files/references
- **Depends on:** All previous phases

#### 7.7 Final verification
- `./gradlew build` for all modules
- Verify all tests pass on all targets
- **Depends on:** 7.6

**Acceptance criteria:** Full project builds. All tests pass. Documentation updated. Publishable artifacts generated for all targets.

---

## Maven Publishing Architecture

With this consolidation, consumers get a dramatically simpler dependency setup:

### For library consumers

```kotlin
// build.gradle.kts — ALL platforms, single dependency
dependencies {
    implementation("io.github.jermeyyy.seal:seal-core:0.2.0")
    // Optional: Ktor integration
    implementation("io.github.jermeyyy.seal:seal-ktor:0.2.0")
}
```

KMP Gradle plugin + Maven metadata automatically resolves the correct platform artifact:
- **Android** → `seal-core-android` (includes Conscrypt/OkHttp integration, AndroidDiskCache)
- **iOS** → `seal-core-iosarm64` / `seal-core-iossimulatorarm64` (includes SecTrust/URLSession integration)
- **JVM Desktop** → `seal-core-jvm` (includes Conscrypt-OpenJDK/OkHttp integration)
- **wasmJs** → `seal-core-wasm-js` (audit-only stubs, minimal footprint)

### Migration from 0.1.0

```diff
dependencies {
-   implementation("io.github.jermeyyy.seal:seal-core:0.1.0")
-   implementation("io.github.jermeyyy.seal:seal-android:0.1.0") // Android only
-   // OR
-   implementation("io.github.jermeyyy.seal:seal-ios:0.1.0") // iOS only
+   implementation("io.github.jermeyyy.seal:seal-core:0.2.0")
}

// Import changes:
- import com.jermey.seal.android.okhttp.* // Old
+ import com.jermey.seal.jvm.okhttp.*     // New (shared Android + JVM)
// iOS imports unchanged — com.jermey.seal.ios.* stays the same
```

---

## Key Risks & Mitigations

| Risk | Severity | Mitigation |
|------|----------|-----------|
| **Conscrypt-OpenJDK reflection API differences** | High | Validate reflection targets exist in conscrypt-openjdk JAR before Phase 2 |
| **jvmSharedMain intermediate source set Gradle configuration** | Medium | Follow official KMP hierarchical source set docs; test incrementally |
| **Demo dependency wasmJs compatibility** (FlowMVI, Koin, Quo Vadis) | High | Check each library's target support; implement fallbacks for unsupported ones |
| **Compose for Web (wasmJs) maturity** | Medium | Accept some UI limitations; use latest Compose Multiplatform 1.10.0 |
| **Package rename breaking change** for existing Android consumers | Medium | Document migration guide in CHANGELOG; accepted by project owner |
| **seal-core artifact size increase** | Low | Platform-specific code only ships in platform artifacts; commonMain stays lean |
| **Publishing convention plugin multi-target support** | Medium | Verify vanniktech plugin handles all KMP targets correctly |

---

## Open Questions

1. **Should `AndroidDiskLogListCache` in `seal-core/src/androidMain/` move to `jvmSharedMain` and rename to `DiskLogListCache`?** — Uses only `java.io.File`. Would provide shared disk cache for both Android and JVM without duplication. **Recommendation: Yes.**
2. **Should there be a `seal-core` `wasmJsTest` source set?** — To run common tests in browser environment. **Recommendation: Yes, configure wasmJs test runner.**
3. **Should `seal-ktor` also be merged into seal-core?** — Decided: No. Ktor stays as a separate optional module to avoid forcing Ktor as a required dependency.

---

## Summary

| Metric | Before | After |
|--------|--------|-------|
| Library modules | 4 (core, android, ios, ktor) | 2 (core, ktor) |
| Targets | 2 (Android, iOS) | 4 (Android, iOS, JVM, wasmJs) |
| Consumer dependencies needed | 2-3 | 1-2 |
| Phases | — | 7 |
| Total tasks | — | ~35 |
