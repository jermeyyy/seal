# Seal — Project Overview

## Purpose
Seal is a **Kotlin Multiplatform (KMP) Certificate Transparency (CT) verification library** for Android and iOS. It verifies Signed Certificate Timestamps (SCTs) to ensure TLS certificates have been publicly logged, protecting against misissued certificates.

## Current State (as of 2026-02-10)
The project is **fully implemented** — all 8 implementation phases are complete. All 4 library modules (`seal-core`, `seal-android`, `seal-ios`, `seal-ktor`) are implemented, tested (core has 12 test classes), and configured for Maven Central publishing. A full-featured demo app exists in `composeApp` + `androidApp`. A documentation website exists under `docs/site/`. Current version: **0.1.0** (initial release, 2026-02-08).

## Architecture
| Module | Type | Targets | Purpose |
|--------|------|---------|---------|
| `:seal-core` | KMP library | Android, iosArm64, iosSimulatorArm64 | Data models, ASN.1/SCT parsing, X.509 parsing, verification engine, CT policies, log list management |
| `:seal-android` | Android library | Android | Conscrypt integration, OkHttp interceptor, TrustManager, OCSP parsing |
| `:seal-ios` | KMP library | iOS | SecTrust evaluation, URLSession delegate helpers |
| `:seal-ktor` | KMP library | Android, iOS | Ktor HttpClient plugin bridging platform implementations |
| `:composeApp` | KMP app | Android, iOS | Demo application (MVI + Compose Multiplatform) |
| `:androidApp` | Android app | Android | Android host activity for demo app |

## Key Design Decisions
- **Fail-open default** — connections are never blocked by CT failures unless user opts into fail-closed
- **Conscrypt required on Android** — for TLS extension and OCSP SCT access
- **Lenient JSON parsing** — `ignoreUnknownKeys = true`, tolerant of schema evolution
- **Hybrid iOS verification** — manual verification of embedded SCTs, OS-level for TLS/OCSP
- **Pure Kotlin ASN.1 parser** — no Bouncy Castle, works in commonMain
- **Bundled log list** — works offline immediately via embedded fallback
- **`explicitApi()` mode** — clean public API surface for Maven Central
- **Convention plugin** — `seal.publishing` in build-logic for Maven Central publishing

## Tech Stack
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **AGP**: 9.0.0
- **Android minSdk**: 24, compileSdk/targetSdk: 36
- **iOS targets**: iosArm64, iosSimulatorArm64
- **kotlinx-serialization-json**: 1.8.1
- **kotlinx-datetime**: 0.7.1
- **kotlinx-io**: 0.7.0
- **kotlinx-coroutines**: 1.10.2
- **Ktor**: 3.4.0
- **OkHttp**: 4.12.0
- **Conscrypt**: 2.5.3
- **Demo app**: FlowMVI 3.2.1, Koin 4.2.0-beta2, Quo Vadis 0.3.4
- **Publishing**: Dokka 2.0.0, Vanniktech Maven Publish 0.34.0

## Repository
- Owner: jermeyyy
- Name: seal
- Branch: main
- License: MIT
- Base package: `com.jermey.seal`
- Publishing group: `io.github.jermeyyy.seal`
