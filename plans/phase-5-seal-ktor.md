# Phase 5: seal-ktor — Ktor Client Plugin

> **Prerequisites**: Phase 1B (Task 1.16), Phase 3 (Task 3.5), Phase 4 (Task 4.4)
> **Summary**: Ktor Client Plugin that provides a unified CT verification API across platforms. Defines the plugin registration with platform-agnostic configuration, then implements platform-specific bridging to OkHttp interceptor (Android) and SecTrust verification (iOS).

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires Phase 1B (Task 1.16), Phase 3 (Task 3.5), Phase 4 (Task 4.4).

---

## Dependency Graph (Phase 5)

```
5.1  Ktor plugin definition       ← 1.16
5.2  Android Ktor (OkHttp)        ← 3.5, 5.1
5.3  iOS Ktor (Darwin)            ← 4.4, 5.1
```

**Parallelization**: Tasks 5.2 + 5.3 can be done in parallel (after 5.1 + respective platform deps).

---

## Tasks

---

### Task 5.1: Define Ktor CertificateTransparency Plugin

**Description**: Create the Ktor `HttpClientPlugin` (formerly `Feature`) registration with platform-agnostic configuration.

**Files to create**:
- `seal-ktor/src/commonMain/kotlin/com/jermey/seal/ktor/CertificateTransparencyPlugin.kt`

**Implementation**:
```kotlin
public val CertificateTransparency = createClientPlugin(
    "CertificateTransparency",
    ::CTConfigurationBuilder
) {
    val config = pluginConfig.build()
    // Platform-specific installation handled via expect/actual
    installPlatformCt(this, config)
}

internal expect fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
)
```

**Dependencies**: 1.16
**Acceptance Criteria**: Plugin installs in Ktor client; configuration DSL works
**Complexity**: Medium

---

### Task 5.2: Android Ktor Implementation (OkHttp Engine)

**Description**: Implement the `actual` platform CT installation for Android that bridges to the OkHttp interceptor.

**Files to create**:
- `seal-ktor/src/androidMain/kotlin/com/jermey/seal/ktor/PlatformCt.android.kt`

**Approach**: On Android with OkHttp engine, configure the engine's `OkHttpConfig` to add the `CertificateTransparencyInterceptor` as a network interceptor.

```kotlin
internal actual fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
) {
    // Hook into OkHttp engine configuration
    // Add CertificateTransparencyInterceptor to the underlying OkHttpClient
}
```

**Note**: This requires careful integration with Ktor's engine configuration mechanism. May need to use `on(SendingRequest)` or engine-specific hooks.

**Dependencies**: 3.5, 5.1
**Acceptance Criteria**: `HttpClient(OkHttp) { install(CertificateTransparency) { ... } }` performs CT checks
**Complexity**: Medium

---

### Task 5.3: iOS Ktor Implementation (Darwin Engine)

**Description**: Implement the `actual` platform CT installation for iOS that bridges to the SecTrust verification.

**Files to create**:
- `seal-ktor/src/iosMain/kotlin/com/jermey/seal/ktor/PlatformCt.ios.kt`

**Approach**: On iOS with Darwin engine, hook into the `handleChallenge` configuration to intercept server trust challenges and delegate to `IosCertificateTransparencyVerifier`.

```kotlin
internal actual fun installPlatformCt(
    pluginInstance: PluginInstance,
    config: CTConfiguration,
) {
    // Hook into Darwin engine's handleChallenge
    // Use IosCertificateTransparencyVerifier for server trust evaluation
}
```

**Dependencies**: 4.4, 5.1
**Acceptance Criteria**: `HttpClient(Darwin) { install(CertificateTransparency) { ... } }` performs CT checks
**Complexity**: Medium
