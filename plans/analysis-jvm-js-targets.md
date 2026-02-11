# Analysis: Adding JVM Desktop and JS/Wasm Browser Targets to Seal

**Date:** 2026-02-11  
**Status:** Complete Investigation

---

## 1. All `expect` Declarations in `seal-core/src/commonMain` Needing New Actuals

### 1a. `createCryptoVerifier()` — `seal-core/src/commonMain/kotlin/com/jermey/seal/core/crypto/CryptoProvider.kt`

```kotlin
public expect fun createCryptoVerifier(): CryptoVerifier
```

**Existing actuals:**
- **androidMain** → `JvmCryptoVerifier()` (uses `java.security.*`)
- **iosMain** → `IosCryptoVerifier()` (uses Apple Security/CommonCrypto)

**New actuals needed:** `jvmMain`, `jsMain` (or `wasmJsMain`)

### 1b. `ResourceLoader` — `seal-core/src/commonMain/kotlin/com/jermey/seal/core/loglist/ResourceLoader.kt`

```kotlin
public expect object ResourceLoader {
    public fun loadResource(name: String): ByteArray
}
```

**Existing actuals:**
- **androidMain** → `ClassLoader.getResourceAsStream(name)` 
- **iosMain** → `NSBundle.mainBundle.pathForResource()`

**New actuals needed:** `jvmMain`, `jsMain` (or `wasmJsMain`)

**Note:** `ResourceLoader` is currently **unused** — no references found in the codebase. `EmbeddedLogListData` embeds the log list JSON as a raw string literal, bypassing `ResourceLoader` entirely. Consider whether to implement actuals or deprecate/remove this expect declaration.

### Summary: Only **2 expect declarations** in `seal-core/commonMain` need new actuals.

---

## 2. CryptoVerifier Expect/Actual Pattern — Crypto APIs for JVM and JS

### Interface Contract (commonMain)

```kotlin
public interface CryptoVerifier {
    fun verifySignature(
        publicKeyBytes: ByteArray, // DER-encoded SubjectPublicKeyInfo
        data: ByteArray,
        signature: ByteArray,
        algorithm: SignatureAlgorithm, // ECDSA or RSA
    ): Boolean

    fun sha256(data: ByteArray): ByteArray
}
```

Algorithms supported: `ECDSA` (SHA256withECDSA) and `RSA` (SHA256withRSA).

### Android Actual (reusable pattern for JVM)

`JvmCryptoVerifier` uses `java.security.*`:
- `KeyFactory.getInstance("EC"/"RSA")` + `X509EncodedKeySpec`
- `Signature.getInstance("SHA256withECDSA"/"SHA256withRSA")`
- `MessageDigest.getInstance("SHA-256")`

### JVM Target Assessment

**The Android actual (`JvmCryptoVerifier`) is 100% reusable on JVM desktop** — it uses only `java.security.*` APIs (`KeyFactory`, `Signature`, `MessageDigest`, `X509EncodedKeySpec`), all available in standard JDK. No Android-specific APIs. No Conscrypt required for basic verification.

**Strategy:** Share the existing `JvmCryptoVerifier` implementation between `androidMain` and `jvmMain` using an intermediate source set (e.g., `jvmCommonMain` / `jvmSharedMain`) or simply duplicate the ~30-line class.

### JS/Wasm Target Assessment

**JS has `SubtleCrypto` (Web Crypto API):**
- `crypto.subtle.verify("ECDSA", key, signature, data)` — **async only** (returns `Promise`)
- `crypto.subtle.importKey("spki", keyData, ...)` — supports SPKI directly
- `crypto.subtle.digest("SHA-256", data)` — **async only**
- Supports both ECDSA P-256 and RSASSA-PKCS1-v1_5

**Challenge:** `CryptoVerifier` interface has **synchronous** methods (`fun verifySignature(...)`, `fun sha256(...)`). The Web Crypto API is **exclusively async**. Options:
1. **Change `CryptoVerifier` to `suspend fun`** — Breaking change but cleanest. Propagates up through `SctSignatureVerifier` and `CertificateTransparencyVerifier` (already suspend-aware for log list loading, but specific verify calls may not be).
2. **Use `kotlinx.coroutines` bridge** — Use `runBlocking` or similar, but this is problematic in JS browser context (no `runBlocking` on JS).
3. **Per the plan context:** "JS/Wasm will use browser's native CT and expose read-only audit results through Seal's API" — If JS doesn't do full CT verification, a **no-op/stub crypto verifier** may suffice, with audit results coming from browser's built-in CT.

**Recommendation:** If JS target is read-only audit (browser handles CT), implement a stub `JsCryptoVerifier` that throws `UnsupportedOperationException` or returns `false`. If full verification is needed, the `CryptoVerifier` interface must become `suspend fun` (breaking change — acceptable per requirements).

---

## 3. Resource Loading (`EmbeddedLogListData`, `ResourceLoader`)

### Current Pattern

`EmbeddedLogListData` is an `internal object` in **commonMain** that contains the **JSON as a raw Kotlin string literal** (~44KB). It does NOT depend on `ResourceLoader` or any file I/O.

`EmbeddedLogListDataSource` reads from `EmbeddedLogListData.json.encodeToByteArray()` — pure Kotlin, fully cross-platform.

**No new actuals needed for embedded log list loading.** It works on all KMP targets already.

### `ResourceLoader` Status

- Expect declaration exists in commonMain with actuals in androidMain/iosMain
- **Zero usages** found in the codebase — it is dead code
- Android actual uses `ClassLoader.getResourceAsStream()` (works on JVM too)
- iOS actual uses `NSBundle.mainBundle`

**Options:**
1. Implement JVM actual (trivial — same as Android classpath loading) and JS actual (e.g., `fetch()` or bundled string)
2. Remove `ResourceLoader` entirely since it's unused
3. Keep it for potential future use with a TODO

**Recommendation:** Implement JVM actual (copy of Android's classpath approach) and a JS stub, or deprecate. Low priority since it's unused.

---

## 4. DiskLogListCache Pattern — Platform APIs for Caching on JVM and JS

### Current Architecture

`DiskLogListCache` is **NOT an expect/actual pattern** — it has no expect declaration in commonMain. Instead:

- **commonMain** defines `LogListCache` interface + `InMemoryLogListCache` (default)
- **androidMain** provides `AndroidDiskLogListCache(cacheDir: java.io.File)` implementing `LogListCache`
- **iosMain** provides `IosDiskLogListCache(cacheDirectoryPath: String)` implementing `LogListCache`

These are **optional, platform-specific implementations** injected via `CTConfigurationBuilder.logListCache`.

### JVM Assessment

**Trivial.** Create `JvmDiskLogListCache(cacheDir: java.io.File)` — identical to `AndroidDiskLogListCache`. The Android implementation uses only `java.io.File`, `readBytes()`, `writeBytes()`, `readLines()`, `writeText()` — all standard JDK APIs.

**Strategy:** Same as CryptoVerifier — share with Android via intermediate source set or duplicate.

### JS/Wasm Assessment

**Options:**
- `localStorage` — synchronous, ~5MB limit, string-only (Base64 encode bytes)
- `IndexedDB` — async, generous storage, but complex API
- `Cache API` — async, designed for request/response pairs
- `Origin Private File System (OPFS)` — async, file-like API

**If JS is audit-only:** A simple `localStorage`-based cache or even `InMemoryLogListCache` may suffice.

**Recommendation:** For JVM, reuse/share the Android implementation. For JS, `InMemoryLogListCache` is sufficient initially (browser context likely doesn't need persistent cache).

---

## 5. seal-ktor's `installPlatformCt()` Expect/Actual — JVM and JS Needs

### Current Pattern

```kotlin
// commonMain
internal expect fun installPlatformCt(
    pluginInstance: ClientPluginBuilder<CTConfigurationBuilder>,
    config: CTConfiguration,
)
```

**Both Android and iOS actuals are empty/no-op** — they just contain comments explaining that platform-specific CT is handled at the engine level, not through plugin hooks.

### Platform-specific extension functions

In addition to `installPlatformCt`, each platform provides a **typed extension function**:
- **Android:** `HttpClientConfig<OkHttpConfig>.certificateTransparency(block)` — configures OkHttp with Conscrypt SSLSocketFactory + `CertificateTransparencyInterceptor`
- **iOS:** `HttpClientConfig<DarwinClientEngineConfig>.certificateTransparency(block)` — configures Darwin's `handleChallenge` for SecTrust CT verification

### JVM Assessment

**Ktor engine options for JVM:**
- `ktor-client-okhttp` — Available on JVM. Can reuse OkHttp-based approach, but **without Conscrypt** (standard JDK TLS doesn't expose SCTs via TLS extension). SCT extraction limited to **embedded SCTs** only (parsed from certificate).
- `ktor-client-cio` — Pure Kotlin engine, no TLS hook points for CT.
- `ktor-client-java` — Uses `java.net.http.HttpClient` (Java 11+), limited TLS hooks.
- `ktor-client-jetty` — Jetty-based, complex TLS configuration.

**JVM CT integration approach:**
1. Use OkHttp engine (available on JVM)
2. Create `seal-jvm` module with a JVM-specific OkHttp interceptor that:
   - Extracts peer certificate chain from OkHttp connection
   - Extracts **embedded SCTs** from leaf certificate (using `seal-core`'s parser)
   - Does NOT get TLS extension SCTs (no Conscrypt on JVM desktop)
   - Runs `CertificateTransparencyVerifier.verify()` with the chain
3. Provide `HttpClientConfig<OkHttpConfig>.certificateTransparency()` Ktor extension

**Key insight:** The `CertificateTransparencyInterceptor` in `seal-android` uses Conscrypt-specific APIs (`ConscryptSctExtractor`, `ConscryptCtSocketFactory`) that DON'T work on JVM desktop. But the **core interceptor logic** (get chain → extract SCTs → verify) can be adapted. TLS extension + OCSP SCTs would need Conscrypt on JVM (possible via `conscrypt-openjdk` artifact).

**Option A (simpler):** JVM interceptor that only verifies embedded SCTs (no Conscrypt dependency)  
**Option B (fuller):** Include `conscrypt-openjdk` for full SCT extraction on JVM desktop

### JS/Wasm Assessment

**Ktor JS engine:** `ktor-client-js` — uses `fetch()` API. No TLS-level hooks at all. The browser handles CT natively.

**Strategy:** `installPlatformCt` for JS should be a no-op. The `certificateTransparency` extension function could be omitted or be a no-op for JS engine. Per the plan, JS exposes read-only audit results only.

### New actuals needed:
- **jvmMain:** `installPlatformCt()` — no-op (same as Android/iOS) + `certificateTransparency()` extension for OkHttp
- **jsMain:** `installPlatformCt()` — no-op + optional stub extension

---

## 6. composeApp Build Configuration and Platform Actuals

### Current Build Configuration (`composeApp/build.gradle.kts`)

**Targets:** `androidLibrary` + `iosArm64` + `iosSimulatorArm64`  
**Plugins:** `kotlinMultiplatform`, `androidMultiplatformLibrary`, `composeMultiplatform`, `composeCompiler`, `kotlinx.serialization`

### Expect Declarations Needing New Actuals (in composeApp)

| Expect | File | Actuals Needed |
|--------|------|----------------|
| `expect fun getPlatform(): Platform` | `Platform.kt` | `jvmMain`, `jsMain`/`wasmJsMain` |
| `expect val isAndroid: Boolean` | `PlatformInfo.kt` | `jvmMain`, `jsMain`/`wasmJsMain` |
| `expect fun createCtHttpClient(...)` | `HttpEngineFactory.kt` | `jvmMain`, `jsMain`/`wasmJsMain` |
| `expect class CtVerificationRepository()` | `CtVerificationRepository.kt` | `jvmMain`, `jsMain`/`wasmJsMain` |

### Assessment of Each Actual

**`getPlatform()`:**
- JVM: Return `"JVM Desktop ${System.getProperty("java.version")}"`
- JS: Return `"Web Browser"` (or `navigator.userAgent` info)

**`isAndroid`:**
- JVM: `false`
- JS: `false`

**`createCtHttpClient()`:**
- JVM: `HttpClient(OkHttp) { certificateTransparency(ctConfig); httpConfig() }` (similar to Android)
- JS: `HttpClient(Js) { httpConfig() }` — no CT plugin needed (browser native)

**`CtVerificationRepository`:**
- JVM: Similar to Android but without OkHttp engine option (or keep it with JVM OkHttp). `availableEngines = listOf(Engine.Ktor)`
- JS: Minimal implementation. `availableEngines = listOf(Engine.Ktor)`. Verification is delegated to browser. Report results as "browser-verified" or "not-applicable".

### Build Changes Needed

```kotlin
// Add JVM desktop target
jvm("desktop")  // or just jvm()

// Add JS/Wasm target
js(IR) {
    browser()
}
// OR
wasmJs {
    browser()
}

// New source set dependencies
desktopMain.dependencies {
    implementation(libs.ktor.client.okhttp)
    implementation(libs.kotlinx.coroutines.swing) // or -javafx
    implementation(project(":seal-jvm")) // new module
}
jsMain.dependencies { // or wasmJsMain
    implementation(libs.ktor.client.js)
}
```

### Compose Desktop Setup

The `composeApp` currently uses `androidMultiplatformLibrary` plugin. For desktop support, this may need to switch to a different configuration because `androidMultiplatformLibrary` implies Android-centric setup. Desktop Compose apps use `compose.desktop.currentOs` for the `compose.desktop` application configuration.

**Risk:** The demo app is currently structured as a library module (`androidMultiplatformLibrary`), not an executable application. Desktop needs a `main()` entry point, and Web needs an HTML entry point. The `androidApp` module serves as the Android application entry. We'll likely need:
- A `desktopApp/` module (or configure `composeApp` to produce desktop executable)
- A `webApp/` module or configure `composeApp` for `wasmJs`/`js` target with browser entry

---

## 7. Current `settings.gradle.kts` Module Includes

```kotlin
include(":composeApp")
include(":androidApp")
include(":seal-core")
include(":seal-android")
include(":seal-ios")
include(":seal-ktor")
```

### Changes Needed

```kotlin
include(":composeApp")
include(":androidApp")
include(":seal-core")
include(":seal-android")
include(":seal-ios")
include(":seal-jvm")     // NEW — JVM desktop CT integration module
include(":seal-ktor")
// Potentially:
// include(":desktopApp") // If separate desktop application module is created
```

---

## 8. Key Version Catalog Entries (`libs.versions.toml`)

### Existing Entries (Relevant)

| Entry | Value | Notes |
|-------|-------|-------|
| `kotlin` | `2.3.0` | Supports all targets |
| `composeMultiplatform` | `1.10.0` | Supports desktop + web |
| `ktor` | `3.4.0` | Has `ktor-client-okhttp`, `ktor-client-cio`, `ktor-client-js` |
| `kotlinx-coroutines` | `1.10.2` | Multi-target |
| `kotlinx-serialization` | `1.8.1` | Multi-target |
| `kotlinx-datetime` | `0.7.1` | Multi-target |
| `kotlinx-io` | `0.7.0` | Multi-target |
| `okhttp` | `4.12.0` | Works on JVM (non-Android) |
| `conscrypt` | `2.5.3` | `conscrypt-android` only; need `conscrypt-openjdk` for JVM |

### New Entries Needed

```toml
[versions]
# No new version needed — existing ktor/okhttp versions work on JVM

[libraries]
# Ktor engines
ktor-client-cio = { module = "io.ktor:ktor-client-cio", version.ref = "ktor" }
ktor-client-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }

# Optional: Conscrypt for JVM desktop (if full SCT extraction is desired)
conscrypt-openjdk = { module = "org.conscrypt:conscrypt-openjdk", version.ref = "conscrypt" }
# Note: conscrypt-openjdk uses the same version as conscrypt-android

# Coroutines for desktop
kotlinx-coroutines-swing = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-swing", version.ref = "kotlinx-coroutines" }
```

---

## 9. seal-android KMP Target Configuration — OkHttp Reusability on JVM

### Current Configuration

`seal-android` targets **Android only** (`androidMultiplatformLibrary` plugin, `androidMain` source set only).

### OkHttp Interceptor Reusability Assessment

**Reusable on JVM (with modifications):**
- `CertificateTransparencyInterceptor` — The core OkHttp `Interceptor` logic. Uses `chain.connection().handshake().peerCertificates` (pure OkHttp API). **But:** depends on `ConscryptSctExtractor` and `android.util.Log`.
- `CertificateChainCleaner` — Uses `java.security.cert.*` APIs. **Fully JVM-compatible.**
- `installCertificateTransparency` DSL — Uses `javax.net.ssl.*` APIs. **But:** requires Conscrypt provider.

**NOT reusable (Android-only):**
- `ConscryptCtSocketFactory` — Depends on `org.conscrypt.Conscrypt`, `android.util.Log`, reflection on Conscrypt internals
- `ConscryptSctExtractor` — Depends on `org.conscrypt.Conscrypt`, `android.util.Log`
- `CTTrustManager` / `CTTrustManagerFactory` — Android TrustManager integration
- `ConscryptInitializer` — Android `androidx.startup.Initializer`
- `OcspResponseParser` — Could work on JVM but tightly coupled to Conscrypt session API
- `AndroidDiskCache` — Uses `android.content.Context`

### Recommended Approach for `seal-jvm`

Create a new `seal-jvm` module that:
1. Contains a simplified `JvmCertificateTransparencyInterceptor` (OkHttp `Interceptor`)
2. Gets certificate chain from OkHttp's `handshake().peerCertificates`
3. Extracts **embedded SCTs** from leaf certificate using `seal-core`'s `SctListParser`
4. Runs `CertificateTransparencyVerifier.verify()` from `seal-core`
5. Does NOT require Conscrypt (uses standard JDK TLS)
6. Optionally supports `conscrypt-openjdk` for TLS extension SCTs

**Shared code between seal-android and seal-jvm:**
- The interceptor logic is similar but seal-android has Conscrypt-specific SCT extraction that seal-jvm won't have
- `CertificateChainCleaner` could be shared via an intermediate source set
- Best approach: create a clean implementation in `seal-jvm` that reuses `seal-core` APIs

---

## 10. New Source Sets Summary

### seal-core

| Source Set | Purpose |
|-----------|---------|
| `jvmMain` | **NEW** — JVM actuals for `createCryptoVerifier()` and `ResourceLoader` |
| `jsMain` or `wasmJsMain` | **NEW** — JS/Wasm actuals (stub or Web Crypto based) |
| `jvmCommonMain` (optional) | Shared between `androidMain` and `jvmMain` — could hold `JvmCryptoVerifier` |

### seal-ktor

| Source Set | Purpose |
|-----------|---------|
| `jvmMain` | **NEW** — `installPlatformCt()` actual + `certificateTransparency()` extension for OkHttp on JVM |
| `jsMain` or `wasmJsMain` | **NEW** — `installPlatformCt()` no-op actual |

### seal-jvm (new module)

| Source Set | Purpose |
|-----------|---------|
| `jvmMain` | JVM OkHttp interceptor, JVM disk cache, JVM-specific CT integration |

### composeApp

| Source Set | Purpose |
|-----------|---------|
| `desktopMain` or `jvmMain` | **NEW** — Desktop platform actuals, `main()` function |
| `jsMain` or `wasmJsMain` | **NEW** — Browser platform actuals, `index.html` entry |

---

## 11. Risks and Challenges

### High Risk

1. **CryptoVerifier sync vs async mismatch on JS** — Web Crypto API is exclusively async. If JS target needs full CT verification, `CryptoVerifier` interface needs `suspend` modifier (breaking change propagating through `SctSignatureVerifier` → `CertificateTransparencyVerifier`). Mitigated if JS is audit-only (stub verifier).

2. **Compose Multiplatform Web maturity** — Compose for Web (wasmJs) is still experimental/alpha. There may be missing components, rendering issues, or dependency compatibility problems. The demo app uses FlowMVI, Koin, Quo Vadis — need to confirm all support JS/Wasm targets.

### Medium Risk

3. **Demo app module restructuring** — `composeApp` currently uses `androidMultiplatformLibrary` plugin. Desktop + Web require different application configurations. May need to split into `composeApp` (shared UI) + platform-specific app modules, or restructure as `composeMultiplatform` application.

4. **OkHttp on JVM without Conscrypt** — Only embedded SCTs will be verifiable. TLS extension and OCSP SCTs require Conscrypt. Most certificates have embedded SCTs, but coverage won't be 100%.

5. **JS/Wasm target choice** — Need to decide between `js(IR) { browser() }` and `wasmJs { browser() }`. WasmJs has better performance but less library ecosystem support. Could support both, but doubles the work.

### Low Risk

6. **ResourceLoader dead code** — Unused expect declaration. Could confuse developers. Recommend removing or documenting intent.

7. **`Engine` enum expansion** — Currently `Ktor` and `OkHttp`. Desktop may want both (OkHttp available on JVM), but JS would only have `Ktor`. The enum is in commonMain — need to handle per-platform availability gracefully (already done via `CtVerificationRepository.availableEngines`).

8. **Version catalog completeness** — Need to add `ktor-client-js`/`ktor-client-cio` entries. Minor but must not be forgotten.

---

## 12. Recommended Implementation Sequence

1. **Phase 1: seal-core JVM target** — Add `jvm()` target, create `jvmMain` source set, implement `createCryptoVerifier()` (copy `JvmCryptoVerifier`) and `ResourceLoader` (copy Android classpath approach). Run existing commonTest on JVM.

2. **Phase 2: seal-core JS/Wasm target** — Add `js(IR) { browser() }` or `wasmJs`, create source set, implement stub `createCryptoVerifier()` and `ResourceLoader`.

3. **Phase 3: seal-jvm module** — Create new module with JVM OkHttp interceptor for CT verification. Depends on `seal-core`.

4. **Phase 4: seal-ktor JVM + JS** — Add JVM and JS targets, implement `installPlatformCt()` actuals and extension functions.

5. **Phase 5: composeApp Desktop** — Add JVM desktop target, implement platform actuals, create desktop `main()` entry point.

6. **Phase 6: composeApp Web** — Add JS/Wasm target, implement platform actuals, create browser entry point.

7. **Phase 7: Build config & catalog** — Update `settings.gradle.kts`, `libs.versions.toml`, all `build.gradle.kts` files.

---

## Appendix: File-by-File Change List

| File | Changes |
|------|---------|
| `settings.gradle.kts` | Add `include(":seal-jvm")` |
| `gradle/libs.versions.toml` | Add `ktor-client-cio`, `ktor-client-js`, optionally `conscrypt-openjdk`, `kotlinx-coroutines-swing` |
| `seal-core/build.gradle.kts` | Add `jvm()` and `js(IR) { browser() }` (or `wasmJs`) targets, new source set dependencies |
| `seal-core/src/jvmMain/` | New: `CryptoProvider.jvm.kt`, `ResourceLoader.jvm.kt` |
| `seal-core/src/jsMain/` (or `wasmJsMain/`) | New: `CryptoProvider.js.kt`, `ResourceLoader.js.kt` |
| `seal-jvm/build.gradle.kts` | New module: KMP with JVM target, depends on `seal-core`, `okhttp` |
| `seal-jvm/src/jvmMain/` | New: JVM OkHttp interceptor, disk cache, DSL extensions |
| `seal-ktor/build.gradle.kts` | Add `jvm()` and `js()`/`wasmJs()` targets, engine dependencies |
| `seal-ktor/src/jvmMain/` | New: `installPlatformCt()` actual, `certificateTransparency()` for OkHttp |
| `seal-ktor/src/jsMain/` (or `wasmJsMain/`) | New: `installPlatformCt()` no-op actual |
| `composeApp/build.gradle.kts` | Add desktop + web targets, new source set dependencies |
| `composeApp/src/desktopMain/` | New: `Platform.desktop.kt`, `PlatformInfo.desktop.kt`, `HttpEngineFactory.desktop.kt`, `CtVerificationRepository.desktop.kt`, `main.kt` |
| `composeApp/src/jsMain/` (or `wasmJsMain/`) | New: Platform actuals for web, `index.kt` entry point |
