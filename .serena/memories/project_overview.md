# Seal — Project Overview

## Purpose
Seal is a **Kotlin Multiplatform (KMP) Certificate Transparency library** for Android and iOS. It verifies Signed Certificate Timestamps (SCTs) to ensure TLS certificates have been publicly logged, protecting against misissued certificates.

## Current State (as of 2026-02-07)
The project is currently a **Compose Multiplatform template** that needs to be restructured into a multi-module library. Only the `composeApp` module exists with default template code (Greeting, Platform expect/actual).

An implementation plan exists at `plans/seal-implementation-plan.md` describing the full transformation.

## Target Architecture
| Module | Type | Targets | Purpose |
|--------|------|---------|---------|
| `:seal-core` | KMP library | Android, iosArm64, iosSimulatorArm64 | Data models, ASN.1/SCT parsing, verification engine, policy |
| `:seal-android` | Android library | Android | Conscrypt integration, OkHttp interceptor, TrustManager |
| `:seal-ios` | KMP library | iOS | SecTrust evaluation, URLSession delegate helpers |
| `:seal-ktor` | KMP library | Android, iOS | Ktor HttpClient plugin bridging platform implementations |
| `:composeApp` | KMP app | Android, iOS | Demo application |

## Key Design Decisions
- **Fail-open default** — connections are never blocked by CT failures unless user opts into fail-closed
- **Conscrypt required on Android** — for TLS extension and OCSP SCT access
- **Lenient JSON parsing** — `ignoreUnknownKeys = true`, tolerant of schema evolution
- **Hybrid iOS verification** — manual verification of embedded SCTs, OS-level for TLS/OCSP
- **Pure Kotlin ASN.1 parser** — no Bouncy Castle, works in commonMain
- **Bundled log list** — works offline immediately
- **`explicitApi()` mode** — clean public API surface for Maven Central

## Tech Stack
- **Kotlin**: 2.3.0
- **Compose Multiplatform**: 1.10.0
- **AGP**: 8.11.2
- **Android minSdk**: 24, compileSdk/targetSdk: 36
- **iOS targets**: iosArm64, iosSimulatorArm64
- **Build system**: Gradle with Kotlin DSL, version catalog (`gradle/libs.versions.toml`)
- **Planned deps**: kotlinx-serialization, kotlinx-datetime, kotlinx-io, kotlinx-coroutines, Ktor, OkHttp, Conscrypt

## Repository
- Owner: jermeyyy
- Name: seal
- Branch: main
- License: Apache 2.0
- Base package: `com.jermey.seal`
- Publishing group: `io.github.jermeyyy.seal`
