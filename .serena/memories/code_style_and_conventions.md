# Seal — Code Style & Conventions

## General
- **Kotlin code style**: Official (`kotlin.code.style=official` in `gradle.properties`)
- **Language**: Kotlin (no Java sources)
- **API mode**: `explicitApi()` in all library modules — all public symbols must have explicit visibility modifiers
- **JVM target**: 11 (for Android target)

## Package Structure
- Base package: `com.jermey.seal`
- Core module: `com.jermey.seal.core` (sub-packages: model, asn1, parser, x509, crypto, verification, policy, config, host, loglist)
- Android module: `com.jermey.seal.android` (sub-packages: okhttp, conscrypt, chain, trust, cache)
- iOS module: `com.jermey.seal.ios` (sub-packages: sectrust, urlsession, cache)
- Ktor module: `com.jermey.seal.ktor`
- Demo app: `com.jermey.seal.demo` (sub-packages: data, mvi, ui, di, navigation)

## Naming Conventions
- Standard Kotlin naming: `PascalCase` for classes, `camelCase` for functions/properties
- `expect`/`actual` pattern for platform-specific code (CryptoVerifier, ResourceLoader, DiskLogListCache, installPlatformCt)
- Sealed class hierarchies for result types (`VerificationResult`, `SctVerificationResult`, `LogListResult`)
- DSL builder pattern for configuration (`ctConfiguration {}`, `certificateTransparencyInterceptor {}`, `installCertificateTransparency {}`)
- `fun interface` for policy abstraction (`CTPolicy`)
- Value classes where appropriate (`LogId`)

## File Organization
- KMP source sets: `commonMain`, `androidMain`, `iosMain`, `commonTest`, `androidHostTest`
- Source root: `src/<sourceSet>/kotlin/com/jermey/seal/`
- Resources: `composeResources/` for Compose resources
- Android resources: standard `res/` directory

## Build Configuration Style
- Gradle Kotlin DSL (`.gradle.kts`)
- Version catalog at `gradle/libs.versions.toml`
- Typesafe project accessors enabled (`TYPESAFE_PROJECT_ACCESSORS`)
- Plugin aliases defined in version catalog
- Configuration cache and build caching enabled
- Convention plugin (`seal.publishing`) in `build-logic/convention/` for Maven Central publishing

## Patterns Used Throughout
- **Fail-open by default** in all verification paths (`failOnError = false`)
- **Lenient parsing** — always use `ignoreUnknownKeys = true` for JSON
- **Internal by default** — only expose intentional public API surface
- Prefer `sealed class`/`sealed interface` for result hierarchies
- Use `expect`/`actual` for platform-specific crypto/system APIs
- No wildcard imports except for Compose (`import ...runtime.*` is acceptable in Compose code)
- `+`/`-` operator overloads on builders for host patterns
- Embedded fallback data (bundled log list for offline operation)
- Cache hierarchy: memory → disk → network → embedded fallback

## Demo App Patterns
- **MVI** architecture via FlowMVI library (Containers + Contracts)
- **DI** via Koin (appModule, containerModule)
- **Navigation** via Quo Vadis library
- **Material3** theming via SealTheme
