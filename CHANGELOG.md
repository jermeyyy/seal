# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.2.0] - 2026-02-11

### Added
- **JVM Desktop target** — `seal-core` and `seal-ktor` now support JVM desktop applications
- **wasmJs Browser target** — `seal-core` and `seal-ktor` now support Kotlin/Wasm in the browser
- `JvmDiskLogListCache` — persistent disk cache for JVM desktop (defaults to `~/.seal/cache`)
- `jvmSharedMain` source set — shared code between Android and JVM Desktop (OkHttp, Conscrypt, TrustManager)
- Compose Desktop demo app target (`./gradlew :composeApp:run`)
- Compose wasmJs demo app target (`./gradlew :composeApp:wasmJsBrowserDevelopmentRun`)
- `CTTrustManager` and `CTTrustManagerFactory` now available on JVM Desktop

### Changed
- **BREAKING:** `seal-android` module removed — all Android platform code consolidated into `seal-core`
- **BREAKING:** `seal-ios` module removed — all iOS platform code consolidated into `seal-core`
- **BREAKING:** Package renames for shared JVM code:
  - `com.jermey.seal.android.okhttp.*` → `com.jermey.seal.jvm.okhttp.*`
  - `com.jermey.seal.android.trust.*` → `com.jermey.seal.jvm.trust.*`
  - `com.jermey.seal.android.ConscryptInitializer` → `com.jermey.seal.jvm.ConscryptInitializer`
  - `com.jermey.seal.android.chain.*` → `com.jermey.seal.jvm.chain.*`
  - `com.jermey.seal.android.conscrypt.*` → `com.jermey.seal.jvm.conscrypt.*`
- `OcspResponseParser` moved from `seal-android` to `seal-core` common code (`com.jermey.seal.core.parser`)
- iOS platform code packages unchanged (`com.jermey.seal.ios.*`)
- Android convenience wrappers (`AndroidDiskCache`) package unchanged (`com.jermey.seal.android.cache`)

### Migration Guide
1. Remove `seal-android` and `seal-ios` dependencies — replace with just `seal-core`
2. Update imports:
   - `com.jermey.seal.android.okhttp.certificateTransparencyInterceptor` → `com.jermey.seal.jvm.okhttp.certificateTransparencyInterceptor`
   - `com.jermey.seal.android.okhttp.installCertificateTransparency` → `com.jermey.seal.jvm.okhttp.installCertificateTransparency`
   - `com.jermey.seal.android.trust.CTTrustManagerFactory` → `com.jermey.seal.jvm.trust.CTTrustManagerFactory`
   - `com.jermey.seal.android.ConscryptInitializer` → `com.jermey.seal.jvm.ConscryptInitializer`
3. iOS imports remain the same (`com.jermey.seal.ios.*`)

## [0.1.0] - 2026-02-08

### Added
- Initial release
