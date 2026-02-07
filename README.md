<p align="center">
  <img src="art/logo.png" width="280" alt="Seal logo" />
</p>

<h1 align="center">Seal</h1>

<p align="center">
  <strong>Certificate Transparency for Kotlin Multiplatform</strong>
</p>

<p align="center">
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3.0-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://www.jetbrains.com/kotlin-multiplatform/"><img src="https://img.shields.io/badge/KMP-Android%20%7C%20iOS-4285F4" alt="Platforms" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License" /></a>
</p>

---

Seal is a Kotlin Multiplatform library that brings [Certificate Transparency](https://certificate.transparency.dev/) (CT) verification to Android and iOS applications. It verifies Signed Certificate Timestamps (SCTs) to ensure that TLS certificates have been publicly logged, protecting your users against misissued certificates.

## Features

- **Cross-platform** — shared verification logic in pure Kotlin, with platform-native crypto
- **All 3 SCT delivery methods** — Embedded X.509 extensions, TLS extensions, and OCSP stapling
- **OkHttp interceptor** — drop-in network interceptor for Android
- **Ktor plugin** — multiplatform `HttpClient` plugin for Android (OkHttp engine) and iOS (Darwin engine)
- **Configurable policies** — built-in Chrome and Apple presets, or define your own
- **Fail-open by default** — connections are never blocked unless you opt in, avoiding app-wide outages
- **Bundled log list** — ships with an embedded CT log list so verification works offline on first launch
- **Host filtering** — include/exclude hosts with wildcard patterns

## Architecture

```
┌──────────────────────────────────────────┐
│              :composeApp                 │
│          (Demo / Test App)               │
└──────┬──────────┬──────────┬─────────────┘
       │          │          │
       ▼          ▼          ▼
┌────────────┐ ┌────────┐ ┌────────────┐
│:seal-android│ │:seal-  │ │ :seal-ios  │
│ OkHttp     │ │ ktor   │ │ SecTrust   │
│ Conscrypt  │ │ Plugin │ │ URLSession │
└─────┬──────┘ └───┬────┘ └─────┬──────┘
      └────────────┼────────────┘
                   ▼
           ┌──────────────┐
           │  :seal-core  │
           │ Models, ASN.1│
           │ SCT Parser   │
           │ Policy Engine│
           └──────────────┘
```

| Module | Type | Targets | Purpose |
|--------|------|---------|---------|
| `:seal-core` | KMP library | Android, iOS | Data models, ASN.1/SCT parsing, verification engine, policy |
| `:seal-android` | Android library | Android | Conscrypt integration, OkHttp interceptor, TrustManager wrapper |
| `:seal-ios` | KMP library | iOS | SecTrust evaluation, URLSession delegate helpers |
| `:seal-ktor` | KMP library | Android, iOS | Ktor `HttpClient` plugin bridging platform implementations |
| `:composeApp` | KMP app | Android, iOS | Demo application |

## Installation

Add the dependencies for the modules you need:

```kotlin
// build.gradle.kts

// Core (required)
commonMain.dependencies {
    implementation("io.github.jermey.seal:seal-core:<version>")
}

// Android — OkHttp interceptor
androidMain.dependencies {
    implementation("io.github.jermey.seal:seal-android:<version>")
}

// iOS — SecTrust / URLSession helpers
iosMain.dependencies {
    implementation("io.github.jermey.seal:seal-ios:<version>")
}

// Ktor plugin (multiplatform)
commonMain.dependencies {
    implementation("io.github.jermey.seal:seal-ktor:<version>")
}
```

## Quick Start

### OkHttp (Android)

```kotlin
import com.jermey.seal.android.okhttp.certificateTransparencyInterceptor

// Zero-config — fail-open, all hosts
val client = OkHttpClient.Builder()
    .addNetworkInterceptor(certificateTransparencyInterceptor())
    .build()
```

### Ktor (Multiplatform)

```kotlin
import com.jermey.seal.ktor.CertificateTransparency

// Android (OkHttp engine)
val client = HttpClient(OkHttp) {
    install(CertificateTransparency)
}

// iOS (Darwin engine)
val client = HttpClient(Darwin) {
    install(CertificateTransparency)
}
```

## Configuration

Both the OkHttp interceptor and the Ktor plugin share the same DSL:

```kotlin
certificateTransparencyInterceptor {
    // Host filtering — if no includes are specified, all hosts are checked
    +"*.example.com"          // include hosts matching pattern
    -"internal.example.com"   // exclude specific hosts (takes precedence)

    // Policy preset
    policy = ChromeCtPolicy()   // or AppleCtPolicy(), or a custom CTPolicy

    // Failure mode
    failOnError = false         // false = fail-open (default), true = fail-closed

    // Logging / reporting
    logger = { host, result ->
        Log.d("CT", "$host: $result")
    }

    // Log list source
    logListUrl = "https://www.gstatic.com/ct/log_list/v3/log_list.json"

    // Disk cache (Android)
    diskCache = AndroidDiskCache(context)
}
```

### Host Patterns

| Pattern | Matches | Does NOT Match |
|---------|---------|----------------|
| `"*.example.com"` | `api.example.com`, `www.example.com` | `example.com` |
| `"**.example.com"` | `deep.sub.example.com` | `example.com` |
| No includes specified | All hosts | — |

Excludes always override includes.

## Policies

### Built-in

```kotlin
// Chrome CT policy
val chrome = ChromeCtPolicy()

// Apple CT policy
val apple = AppleCtPolicy()
```

### Custom

```kotlin
val custom = CTPolicy { certificateLifetimeDays, sctResults ->
    val validFromDistinct = sctResults
        .filterIsInstance<SctVerificationResult.Valid>()
        .distinctBy { it.logOperator }
    if (validFromDistinct.size >= 2) {
        VerificationResult.Success.Trusted(validFromDistinct)
    } else {
        VerificationResult.Failure.TooFewDistinctOperators(
            found = validFromDistinct.size,
            required = 2,
        )
    }
}
```

## Verification Results

Seal reports results through a sealed class hierarchy:

```kotlin
sealed class VerificationResult {
    sealed class Success : VerificationResult() {
        data class Trusted(val scts: List<SctVerificationResult.Valid>)
        data object InsecureConnection
        data object DisabledForHost
    }
    sealed class Failure : VerificationResult() {
        data object NoScts
        data class TooFewSctsTrusted(val found: Int, val required: Int)
        data class TooFewDistinctOperators(val found: Int, val required: Int)
        data class LogServersFailed(val errors: List<SctVerificationResult>)
        data class UnknownError(val cause: Throwable)
    }
}
```

## Platform Notes

### Android

- **Minimum SDK**: 24 (Android 7.0)
- **Conscrypt** is required for TLS extension and OCSP SCT access. Initialize it early:
  ```kotlin
  class MyApplication : Application() {
      override fun onCreate() {
          super.onCreate()
          ConscryptInitializer.initialize()
      }
  }
  ```
- All 3 SCT delivery methods are fully supported via the OkHttp interceptor.
- A `CTTrustManager` wrapper is available for WebView and non-OkHttp use cases (embedded SCTs only).

### iOS

- Embedded SCTs are verified manually by the library against its own log list.
- TLS extension and OCSP stapled SCTs are checked via the OS-level `kSecTrustCertificateTransparency` flag through Security.framework.
- A `UrlSessionCtHelper` is provided for use in `URLSessionDelegate.didReceiveChallenge`.

## Design Principles

| Principle | Details |
|-----------|---------|
| **Fail-open by default** | Connections are never dropped due to CT failures unless `failOnError = true`. This prevents outages from stale log lists or transient issues. |
| **Lenient parsing** | The log list JSON parser uses `ignoreUnknownKeys` and tolerates empty arrays, avoiding crashes from schema evolution. |
| **Bundled log list** | An embedded log list ships with every release, so verification works immediately without network access. |
| **No Bouncy Castle** | A lightweight, pure-Kotlin ASN.1 DER parser keeps the binary size small. Platform-native APIs handle all cryptographic operations. |

## Building

```shell
# Build all modules
./gradlew assemble

# Run tests
./gradlew allTests

# Build the demo app (Android)
./gradlew :composeApp:assembleDebug
```

For the iOS demo app, open the `iosApp/` directory in Xcode and run from there.

## Contributing

Contributions are welcome! Please open an issue first to discuss what you'd like to change.

## License

```
Copyright 2026 Jermey

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

Learn more about [Certificate Transparency](https://certificate.transparency.dev/) · [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
