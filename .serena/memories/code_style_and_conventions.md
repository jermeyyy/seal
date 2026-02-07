# Seal — Code Style & Conventions

## General
- **Kotlin code style**: Official (`kotlin.code.style=official` in `gradle.properties`)
- **Language**: Kotlin (no Java sources)
- **API mode**: `explicitApi()` in all library modules — all public symbols must have explicit visibility modifiers
- **JVM target**: 11 (for Android target)

## Package Structure
- Base package: `com.jermey.seal`
- Core module: `com.jermey.seal.core`
- Android module: `com.jermey.seal.android`
- iOS module: `com.jermey.seal.ios`
- Ktor module: `com.jermey.seal.ktor`

## Naming Conventions
- Standard Kotlin naming: `PascalCase` for classes, `camelCase` for functions/properties
- `expect`/`actual` pattern used for platform-specific code (seen with `Platform` interface)
- Sealed class hierarchies for result types (e.g., `VerificationResult`)
- DSL builder pattern for configuration (e.g., `certificateTransparencyInterceptor { ... }`)

## File Organization
- KMP source sets: `commonMain`, `androidMain`, `iosMain`, `commonTest`
- Source root: `src/<sourceSet>/kotlin/com/jermey/seal/`
- Resources: `composeResources/` for Compose resources
- Android resources: standard `res/` directory

## Build Configuration Style
- Gradle Kotlin DSL (`.gradle.kts`)
- Version catalog at `gradle/libs.versions.toml`
- Typesafe project accessors enabled (`TYPESAFE_PROJECT_ACCESSORS`)
- Plugin aliases defined in version catalog
- Configuration cache and build caching enabled

## Patterns to Follow
- **Fail-open by default** in all verification paths
- **Lenient parsing** — always use `ignoreUnknownKeys = true` for JSON
- **Internal by default** — only expose intentional public API surface
- Prefer `sealed class`/`sealed interface` for result hierarchies
- Use `expect`/`actual` for platform-specific crypto/system APIs
- No wildcard imports except for Compose (`import ...runtime.*` is acceptable in Compose code)
