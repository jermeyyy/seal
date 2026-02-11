# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Seal is a **Kotlin Multiplatform (KMP) Certificate Transparency (CT) verification library** that verifies Signed Certificate Timestamps (SCTs) during HTTPS connections. It targets **Android**, **iOS**, **JVM Desktop**, and **Web (wasmJs)**.

Published to Maven Central under group `io.github.jermeyyy` with artifacts `seal-core` and `seal-ktor`.

## Build & Test Commands

```bash
# Build all modules
./gradlew assemble

# Run all tests across all modules and targets
./gradlew allTests

# Module-specific tests
./gradlew :seal-core:allTests
./gradlew :seal-core:testDebugUnitTest        # Android unit tests
./gradlew :seal-core:jvmTest                   # JVM tests
./gradlew :seal-core:iosSimulatorArm64Test     # iOS simulator tests

# Run demo app
./gradlew :composeApp:assembleDebug            # Android
./gradlew :composeApp:run                       # Desktop JVM
./gradlew :composeApp:wasmJsBrowserDevelopmentRun  # Web

# Generate API docs
./gradlew dokkaGeneratePublicationHtml

# Publish to Maven Local for local testing
./gradlew publishToMavenLocal
```

iOS demo: open `iosApp/iosApp.xcodeproj` in Xcode.

## Architecture

```
:seal-core    — KMP library (Android, iOS, JVM, wasmJs)
:seal-ktor    — Ktor HttpClient plugin (Android, iOS, JVM, wasmJs)
:composeApp   — Compose Multiplatform demo app
:androidApp   — Android host activity for demo
```

`seal-core` contains all verification logic and platform integrations. `seal-ktor` depends on `seal-core` and provides a Ktor plugin. The former `seal-android` and `seal-ios` modules were consolidated into `seal-core` platform source sets.

### seal-core source set hierarchy

- **commonMain** — Data models, ASN.1/DER parser, SCT parsing, X.509 parsing, verification engine, CT policies (`ChromeCtPolicy`, `AppleCtPolicy`), configuration DSL, host matching, log list management
- **jvmSharedMain** (depends on commonMain) — OkHttp interceptor, Conscrypt SCT extraction, OCSP parsing, TrustManager, CertificateChainCleaner. Shared between Android and JVM Desktop.
- **androidMain** (depends on jvmSharedMain) — JCE crypto, Android resource loading, disk cache, Conscrypt Android
- **jvmMain** (depends on jvmSharedMain) — Conscrypt openjdk-uber, JVM disk cache
- **iosMain** — Security framework crypto, SecTrust evaluation, URLSession helpers, NSBundle resources
- **wasmJsMain** — Browser integration (CT handled natively by browsers; audit-mode only)
- **commonTest** — 12 test classes covering ASN.1, host matching, log list, config, SCT parsing, X.509, verification, policy
- **jvmSharedTest** — OkHttp interceptor integration tests

### Key packages in seal-core commonMain (`com.jermey.seal.core.*`)

| Package | Purpose |
|---------|---------|
| `model` | SCT, VerificationResult (sealed hierarchy), LogServer, LogId |
| `asn1` | Pure-Kotlin ASN.1 DER parser (no Bouncy Castle) |
| `parser` | SctListParser, SctDeserializer |
| `x509` | CertificateParser, ParsedCertificate |
| `crypto` | CryptoVerifier expect/actual interface |
| `verification` | CertificateTransparencyVerifier, SctSignatureVerifier |
| `policy` | CTPolicy fun interface, ChromeCtPolicy, AppleCtPolicy |
| `config` | CTConfiguration, CTConfigurationBuilder DSL |
| `host` | HostPattern, HostMatcher (wildcard matching) |
| `loglist` | LogListService, cache hierarchy (memory → disk → network → embedded fallback) |

## Key Design Decisions

- **Fail-open default** — connections are never blocked by CT failures unless `failOnError = true`
- **`explicitApi()` mode** on all library modules — every public symbol needs an explicit visibility modifier
- **Conscrypt required on Android/JVM** — for TLS extension and OCSP SCT access
- **Pure Kotlin ASN.1 parser** — no Bouncy Castle dependency, works in commonMain
- **Bundled log list** — embedded fallback for offline operation
- **Lenient JSON parsing** — always `ignoreUnknownKeys = true`
- **`expect`/`actual` pattern** for platform-specific code: `CryptoVerifier`, `ResourceLoader`, `DiskLogListCache`, `installPlatformCt`
- **DSL builder pattern** for configuration: `ctConfiguration {}`, `certificateTransparencyInterceptor {}`, `installCertificateTransparency {}`
- **Sealed class hierarchies** for result types: `VerificationResult`, `SctVerificationResult`, `LogListResult`

## Code Conventions

- Kotlin official style (`kotlin.code.style=official`)
- Base package: `com.jermey.seal`
- JVM target: 11
- Android: minSdk 24, compileSdk 36
- Gradle Kotlin DSL with version catalog at `gradle/libs.versions.toml`
- Typesafe project accessors enabled
- Convention plugin `seal.publishing` in `build-logic/convention/` handles Maven Central publishing via Vanniktech
- `+`/`-` operator overloads on builders for host include/exclude patterns
- `internal` by default — only expose intentional public API
- No wildcard imports except in Compose code

## Build Infrastructure

- **Version catalog**: `gradle/libs.versions.toml` (Kotlin 2.3.0, Compose 1.10.0, AGP 9.0.0, Ktor 3.4.0)
- **Convention plugin**: `build-logic/convention/` — `seal.publishing.gradle.kts` applies Vanniktech Maven Publish
- **CI**: `.github/workflows/publish-release.yml` (Maven Central publishing on GitHub Release) and `deploy-pages.yml` (docs site)
- **Release process**: documented in `.github/prompts/release.prompt.md` — bump versions in build files + docs constants, update CHANGELOG, tag, push, create GitHub Release
- **Docs site**: React/Vite app at `docs/site/` (`npm run dev` / `npm run build`)

## IDE Notes

- IDE may show false-positive KMP errors — **trust Gradle** build output over IDE diagnostics
- Configuration cache and build caching are enabled in `gradle.properties`
