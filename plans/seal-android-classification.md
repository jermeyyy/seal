# seal-android Module: File Classification Report

**Date:** 2026-02-11  
**Context:** Eliminating planned `seal-jvm` module. Instead, `seal-core` gains a JVM target with an intermediate source set (`jvmSharedMain`) shared between `androidMain` and `jvmMain`. This report classifies every file in `seal-android` as "shared JVM" (portable to `jvmSharedMain`) or "Android-only" (must remain in `seal-android`).

---

## 1. Build Configuration Analysis

### seal-android/build.gradle.kts

| Property | Value |
|----------|-------|
| **Plugin** | `kotlinMultiplatform` + `androidMultiplatformLibrary` |
| **Target** | Android only (single-target KMP module) |
| **API mode** | `explicitApi()` |
| **Android settings** | namespace `com.jermey.seal.android`, compileSdk from catalog, minSdk from catalog, JVM 11 |
| **Testing** | `withHostTest {}` (JVM-hosted Android tests) |

### Dependencies

| Scope | Dependency | JVM-Portable? | Notes |
|-------|-----------|---------------|-------|
| `api` | `project(":seal-core")` | ✅ | Core CT library |
| `implementation` | `libs.conscrypt.android` | ❌ artifact / ✅ API | API is identical to `conscrypt-openjdk`; only the Maven artifact differs |
| `implementation` | `libs.okhttp` | ✅ | Pure JVM library |
| `implementation` | `libs.kotlinx.coroutines.android` | ❌ | Android Dispatchers; JVM uses `kotlinx-coroutines-core` |
| test `implementation` | `libs.kotlin.test`, `libs.kotlin.testJunit` | ✅ | Standard Kotlin test |
| test `implementation` | `libs.okhttp.mockwebserver` | ✅ | Pure JVM |
| test `implementation` | `libs.kotlinx.coroutines.test` | ✅ | KMP test utilities |

**Key insight:** The `kotlinx-coroutines-android` dependency is declared but **no file actually uses `Dispatchers.Main` or any Android-specific coroutine dispatcher**. All coroutine usage is `runBlocking {}`. This dependency can be dropped from the shared source set; only `kotlinx-coroutines-core` is needed.

---

## 2. File Classification Table

### Legend
- ✅ **Shared** → Move to `seal-core` intermediate source set (`jvmSharedMain`)
- ⚠️ **Almost Shared** → Portable after trivial `android.util.Log` removal
- ❌ **Android-only** → Stays in `seal-android`

| # | File | Classification | Android Imports | Reasoning |
|---|------|---------------|----------------|-----------|
| 1 | `cache/AndroidDiskCache.kt` | ❌ Android-only | `android.content.Context` | Uses `Context` to resolve cache directory. Thin wrapper around `seal-core`'s `AndroidDiskLogListCache(File)` |
| 2 | `chain/CertificateChainCleaner.kt` | ✅ Shared | None | Pure `java.security.cert.X509Certificate` chain ordering. Zero Android APIs |
| 3 | `conscrypt/ConscryptSctExtractor.kt` | ⚠️ Almost Shared | `android.util.Log` | Conscrypt reflection API (`getPeerSignedCertificateTimestamp`, `getStatusResponses`) is identical in `conscrypt-openjdk`. Only blocker: `android.util.Log` for debug logging |
| 4 | `conscrypt/OcspResponseParser.kt` | ✅ Shared | None | Uses only `seal-core` ASN.1 parser and `SctListParser`. Could even move to `commonMain` — no JVM-specific APIs at all |
| 5 | `ConscryptInitializer.kt` | ✅ Shared | None | `org.conscrypt.Conscrypt.newProvider()`, `java.security.Security.insertProviderAt()`, `javax.net.ssl.SSLSocket` — all standard JVM + Conscrypt APIs |
| 6 | `trust/CTTrustManagerFactory.kt` | ✅ Shared | None | `java.security.KeyStore`, `javax.net.ssl.TrustManagerFactory`, `javax.net.ssl.X509TrustManager` — standard JVM |
| 7 | `trust/CTTrustManager.kt` | ✅ Shared | None | `javax.net.ssl.X509ExtendedTrustManager`, `java.security.cert.*`, `java.net.Socket`, `javax.net.ssl.SSLEngine` — standard JVM |
| 8 | `okhttp/CertificateTransparencyDsl.kt` | ✅ Shared | None | `okhttp3.*`, `javax.net.ssl.*`, `java.security.*`, `org.conscrypt.Conscrypt` — all portable |
| 9 | `okhttp/ConscryptCtSocketFactory.kt` | ⚠️ Almost Shared | `android.util.Log` | Conscrypt internal reflection (`sslParameters`, `setSignedCertTimestamps`) uses same class hierarchy in `conscrypt-openjdk`. Only blocker: `android.util.Log` |
| 10 | `okhttp/CertificateTransparencyInterceptor.kt` | ✅ Shared | None | `okhttp3.Interceptor`, `javax.net.ssl.*`, seal-core types. No direct Android imports |

### Test File

| File | Classification | Android Imports | Reasoning |
|------|---------------|----------------|-----------|
| `androidHostTest/.../CertificateTransparencyInterceptorTest.kt` | ✅ Shared | None | Pure OkHttp/Kotlin test with fake chain. No Android APIs |

---

## 3. Detailed Import Analysis

### Files with `android.*` imports

Only **3 files** import from `android.*` packages:

| File | Import | Usage | Replacement Strategy |
|------|--------|-------|---------------------|
| `cache/AndroidDiskCache.kt` | `android.content.Context` | `context.cacheDir` to get `File` | **Cannot be shared** — `Context` is fundamentally Android. Stays in `seal-android` as a thin wrapper |
| `conscrypt/ConscryptSctExtractor.kt` | `android.util.Log` | `Log.d(...)`, `Log.e(...)` — 8 call sites | Replace with platform-agnostic logger (see §5) |
| `okhttp/ConscryptCtSocketFactory.kt` | `android.util.Log` | `Log.d(...)`, `Log.w(...)` — 3 call sites | Replace with platform-agnostic logger (see §5) |

### Conscrypt API Portability Verification

All Conscrypt usages in `seal-android` use these APIs:

| API | File(s) | In `conscrypt-openjdk`? |
|-----|---------|------------------------|
| `Conscrypt.newProvider()` | `ConscryptInitializer`, `CertificateTransparencyDsl` | ✅ Yes — `org.conscrypt.Conscrypt` |
| `Conscrypt.isConscrypt(socket)` | `ConscryptSctExtractor`, `ConscryptCtSocketFactory`, `ConscryptInitializer` | ✅ Yes |
| `Security.insertProviderAt(provider, 1)` | `ConscryptInitializer` | ✅ Yes — `java.security.Security` |
| Reflection: `session.getPeerSignedCertificateTimestamp()` | `ConscryptSctExtractor` | ✅ Yes — same `ConscryptSession` interface |
| Reflection: `session.getStatusResponses()` | `ConscryptSctExtractor` | ✅ Yes — same `ConscryptSession` interface |
| Reflection: `sslParameters.setSignedCertTimestamps(true)` | `ConscryptCtSocketFactory` | ✅ Yes — same `SSLParametersImpl` internal class |
| `SSLContext.getInstance("TLS", Conscrypt.newProvider())` | `CertificateTransparencyDsl` | ✅ Yes — standard JSSE SPI |

**Conclusion:** All Conscrypt APIs used are portable between `conscrypt-android` and `conscrypt-openjdk`. The Java API surface is identical; only the native library (BoringSSL) packaging differs.

---

## 4. Dependency Graph Between Files

```
CertificateTransparencyDsl (okhttp/)
├── CertificateTransparencyInterceptor (okhttp/)
│   ├── ConscryptSctExtractor (conscrypt/)
│   │   └── OcspResponseParser (conscrypt/)  [uses seal-core ASN.1]
│   ├── CertificateChainCleaner (chain/)
│   └── seal-core: CertificateTransparencyVerifier, LogListService, CTConfiguration
├── ConscryptCtSocketFactory (okhttp/)
│   └── org.conscrypt.Conscrypt
└── seal-core: CTConfigurationBuilder, ctConfiguration()

CTTrustManagerFactory (trust/)
└── CTTrustManager (trust/)
    ├── CertificateChainCleaner (chain/)
    └── seal-core: CertificateTransparencyVerifier, LogListService, CTConfiguration

ConscryptInitializer
└── org.conscrypt.Conscrypt

AndroidDiskCache (cache/)
└── seal-core: AndroidDiskLogListCache (takes java.io.File)
```

### Dependency clusters (for move ordering)

**Cluster A — OkHttp interceptor stack** (can move together):
1. `OcspResponseParser` (leaf — no local deps)
2. `CertificateChainCleaner` (leaf — no local deps)
3. `ConscryptSctExtractor` → depends on `OcspResponseParser`
4. `ConscryptCtSocketFactory` (leaf — only Conscrypt)
5. `CertificateTransparencyInterceptor` → depends on `ConscryptSctExtractor`, `CertificateChainCleaner`
6. `CertificateTransparencyDsl` → depends on `CertificateTransparencyInterceptor`, `ConscryptCtSocketFactory`

**Cluster B — TrustManager stack** (can move together):
1. `CTTrustManager` → depends on `CertificateChainCleaner` (from Cluster A)
2. `CTTrustManagerFactory` → depends on `CTTrustManager`

**Cluster C — Standalone**:
1. `ConscryptInitializer` (no local deps)

**Stays in seal-android**:
1. `AndroidDiskCache` (Android-only)

---

## 5. Recommendation: What Moves, What Stays

### Moves to `seal-core` jvmSharedMain (9 files)

| File | Pre-move Action Required |
|------|------------------------|
| `chain/CertificateChainCleaner.kt` | None — move as-is |
| `conscrypt/OcspResponseParser.kt` | None — move as-is (consider `commonMain` in future) |
| `conscrypt/ConscryptSctExtractor.kt` | Replace `android.util.Log` with platform logger (see below) |
| `ConscryptInitializer.kt` | None — move as-is |
| `trust/CTTrustManagerFactory.kt` | None — move as-is |
| `trust/CTTrustManager.kt` | None — move as-is |
| `okhttp/CertificateTransparencyDsl.kt` | None — move as-is |
| `okhttp/ConscryptCtSocketFactory.kt` | Replace `android.util.Log` with platform logger (see below) |
| `okhttp/CertificateTransparencyInterceptor.kt` | None — move as-is |

### Stays in seal-android (1 file)

| File | Reason |
|------|--------|
| `cache/AndroidDiskCache.kt` | Uses `android.content.Context` for cache directory resolution |

### Test: Moves to jvmSharedTest (1 file)

| File | Pre-move Action Required |
|------|------------------------|
| `CertificateTransparencyInterceptorTest.kt` | None — move as-is |

### Logging Strategy for `android.util.Log` Removal

Two files need `android.util.Log` replaced. Options:

| Option | Pros | Cons |
|--------|------|------|
| **A: `expect`/`actual` logger in jvmSharedMain** | Type-safe, clean separation | Adds complexity; need actuals in both `androidMain` and `jvmMain` |
| **B: `java.util.logging.Logger`** | Standard JVM, works on both Android (via JUL) and desktop | Android JUL redirects to Logcat anyway; slightly more verbose API |
| **C: Simple function parameter / callback** | No platform dependency at all | Changes internal API surface |
| **D: Remove logging entirely** | Simplest; these are debug/diagnostic logs | Loss of diagnostic info |

**Recommendation: Option B** (`java.util.logging.Logger`). It's standard JVM, available on both Android and desktop, and the existing logs are purely diagnostic (debug/warning level). On Android, JUL messages end up in Logcat anyway. This avoids any `expect`/`actual` overhead for what amounts to 11 log call sites across 2 files.

---

## 6. Impact on seal-android After Move

After moving 9 files out, `seal-android` becomes:

```
seal-android/src/androidMain/kotlin/com/jermey/seal/android/
└── cache/
    └── AndroidDiskCache.kt   ← Only remaining file
```

`seal-android` becomes a **thin Android convenience wrapper**:
- Provides `AndroidDiskCache(context: Context)` for users who want Context-based cache setup
- Re-exports the shared Conscrypt/OkHttp APIs from `seal-core`'s JVM shared source set

### seal-android build.gradle.kts changes needed:
- `api(project(":seal-core"))` stays (but now seal-core provides the OkHttp/Conscrypt classes)
- `implementation(libs.conscrypt.android)` — can potentially become `api` or move to seal-core's `androidMain` dependencies
- `implementation(libs.okhttp)` — moves to seal-core's `jvmSharedMain` dependencies
- `implementation(libs.kotlinx.coroutines.android)` — can be dropped (not actually used by remaining code)
- Test dependencies move with the test file

---

## 7. Summary Statistics

| Metric | Count |
|--------|-------|
| Total source files in `seal-android/src/androidMain/` | 10 |
| Files classified as **Shared JVM** (move to seal-core) | 7 |
| Files classified as **Almost Shared** (move after `Log` fix) | 2 |
| Files classified as **Android-only** (stay) | 1 |
| Test files (shared) | 1 |
| Total `android.*` import sites | 11 (across 2 files using `Log` + 1 file using `Context`) |
| Conscrypt APIs confirmed portable | 7/7 (100%) |

---

## 8. Open Questions

1. **Package naming after move**: Should the shared code keep `com.jermey.seal.android.*` package or be renamed to `com.jermey.seal.jvm.*` / `com.jermey.seal.conscrypt.*`? Renaming is a breaking API change for existing Android users.
   
2. **OcspResponseParser scope**: This file has zero JVM-specific dependencies — it only uses `seal-core`'s ASN.1 parser. Could it live in `commonMain` instead of `jvmSharedMain`? (Low priority; `jvmSharedMain` is fine for now since OCSP stapling is a Conscrypt/JVM concept.)

3. **`AndroidDiskLogListCache` in seal-core**: `seal-core/src/androidMain/` already has `AndroidDiskLogListCache` which uses `java.io.File` (not `Context`). This class could also move to `jvmSharedMain` (rename to `DiskLogListCache`) to be shared with JVM desktop, eliminating the `JvmDiskLogListCache` planned in the old plan.
