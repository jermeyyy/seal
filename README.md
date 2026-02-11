<p align="center">
  <img src="art/logo.png" width="280" alt="Seal logo" />
</p>

<h1 align="center">Seal</h1>

<p align="center">
  <strong>Certificate Transparency for Kotlin Multiplatform</strong>
</p>

<p align="center">
  <a href="https://jermeyyy.github.io/seal/"><img src="https://img.shields.io/badge/Docs-jermeyyy.github.io%2Fseal-blue?logo=gitbook&logoColor=white" alt="Documentation" /></a>
  <a href="https://central.sonatype.com/search?q=io.github.jermeyyy"><img src="https://img.shields.io/maven-central/v/io.github.jermeyyy/seal-core?label=Maven%20Central&logo=apache-maven&color=blue" alt="Maven Central" /></a>
  <a href="https://kotlinlang.org"><img src="https://img.shields.io/badge/Kotlin-2.3.0-7f52ff?logo=kotlin&logoColor=white" alt="Kotlin" /></a>
  <a href="https://www.jetbrains.com/kotlin-multiplatform/"><img src="https://img.shields.io/badge/Platform-Android%20%7C%20iOS%20%7C%20JVM%20Desktop%20%7C%20Web-4285F4?logo=kotlin&logoColor=white" alt="Platforms" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-blue.svg" alt="License" /></a>
</p>

---

Seal is a **Kotlin Multiplatform** library that brings [Certificate Transparency](https://certificate.transparency.dev/) (CT) verification to **Android**, **iOS**, **JVM Desktop**, and **Web (wasmJs)** applications.
It verifies Signed Certificate Timestamps (SCTs) to ensure TLS certificates have been publicly logged, protecting users against misissued or rogue certificates.

## What is Certificate Transparency?

Certificate Transparency is a public framework of append-only **certificate logs** that allows anyone to monitor the certificates issued for their domains.
When a Certificate Authority (CA) issues a TLS certificate, it submits the certificate to one or more CT logs and receives a **Signed Certificate Timestamp (SCT)** — a cryptographic proof that the certificate has been logged.

Seal verifies these SCTs during HTTPS connections to ensure that:

1. The certificate was publicly logged before use.
2. Enough independent log operators have witnessed the certificate (as required by the configured policy).
3. The SCT signatures are valid and from trusted logs.

Without CT enforcement, a compromised or rogue CA can issue certificates that silently intercept traffic.
Seal closes this gap at the application layer.

## Features

- **Cross-platform** — shared verification logic in pure Kotlin with platform-native crypto
- **4 targets** — Android, iOS, JVM Desktop, and Web (wasmJs)
- **All 3 SCT delivery methods** — X.509 embedded extensions, TLS extensions, and OCSP stapling
- **OkHttp interceptor** — drop-in network interceptor for Android and JVM Desktop
- **Ktor plugin** — multiplatform `HttpClient` plugin for all platforms
- **TrustManager wrapper** — `CTTrustManager` for Android and JVM Desktop
- **Configurable policies** — built-in Chrome and Apple presets, or define your own
- **Fail-open by default** — connections are never blocked unless you opt in
- **Bundled log list** — ships with an embedded CT log list so verification works offline on first launch
- **Host filtering** — include/exclude hosts with wildcard patterns
- **No Bouncy Castle** — lightweight, pure-Kotlin ASN.1 DER parser; platform APIs handle crypto

## Architecture

```mermaid
graph TD
    composeApp[":composeApp<br>(Demo App)<br>Android · iOS · Desktop · Web"]
    ktor[":seal-ktor<br>Ktor Plugin<br>Android · iOS · JVM · wasmJs"]
    core[":seal-core<br>Models · ASN.1 · SCT Parser<br>Policy Engine · Platform Integrations<br>Android · iOS · JVM · wasmJs"]

    composeApp --> ktor
    composeApp --> core
    ktor --> core
```

| Module | Type | Targets | Purpose |
|--------|------|---------|---------|
| `seal-core` | KMP library | Android, iOS, JVM, wasmJs | Data models, ASN.1/SCT parsing, verification engine, CT policies, platform integrations (OkHttp, Conscrypt, SecTrust, URLSession) |
| `seal-ktor` | KMP library | Android, iOS, JVM, wasmJs | Ktor `HttpClient` plugin bridging platform implementations |

### Which module do I need?

| Use case | Module(s) |
|----------|-----------|
| Android app with OkHttp | `seal-core` |
| iOS app with URLSession | `seal-core` |
| JVM Desktop app with OkHttp | `seal-core` |
| Any platform with Ktor | `seal-core` + `seal-ktor` |
| Web (wasmJs) | `seal-core` (browser handles CT natively) |

## Installation

### Version Catalog

Add the Seal versions to your `libs.versions.toml`:

```toml
[versions]
seal = "<version>"

[libraries]
seal-core = { module = "io.github.jermeyyy:seal-core", version.ref = "seal" }
seal-ktor = { module = "io.github.jermeyyy:seal-ktor", version.ref = "seal" }
```

### Gradle Dependencies

```kotlin
// build.gradle.kts

// Core — includes all platform integrations
commonMain.dependencies {
    implementation(libs.seal.core)
}

// Ktor plugin (optional — multiplatform)
commonMain.dependencies {
    implementation(libs.seal.ktor)
}
```

Or with raw coordinates:

```kotlin
implementation("io.github.jermeyyy:seal-core:<version>")
implementation("io.github.jermeyyy:seal-ktor:<version>")   // Optional - Ktor plugin
```

## Quick Start — OkHttp (Android & JVM Desktop)

```kotlin
import com.jermey.seal.jvm.okhttp.certificateTransparencyInterceptor

val client = OkHttpClient.Builder()
    .addNetworkInterceptor(
        certificateTransparencyInterceptor {
            // All hosts verified by default — no configuration required.
        }
    )
    .build()
```

> **Important:** The interceptor **must** be added as a **network interceptor** (not an application interceptor) so it runs after TLS negotiation and has access to the certificate chain.

## Quick Start — Ktor

```kotlin
import com.jermey.seal.ktor.CertificateTransparency

// Android / JVM Desktop (OkHttp engine)
val client = HttpClient(OkHttp) {
    install(CertificateTransparency) {
        +"*.example.com"
        -"internal.example.com"
        failOnError = false
    }
}

// iOS (Darwin engine)
val client = HttpClient(Darwin) {
    install(CertificateTransparency) {
        +"*.example.com"
        -"internal.example.com"
        failOnError = false
    }
}

// Web (Js engine) — browser handles CT natively
val client = HttpClient(Js) {
    install(CertificateTransparency)
}
```

The Ktor plugin uses the same configuration DSL on all platforms. The underlying implementation delegates to the appropriate platform engine automatically.

For full guide, see [Ktor Integration](https://jermeyyy.github.io/seal/guides/ktor).

## OkHttp Integration

The OkHttp integration provides a `CertificateTransparencyInterceptor` that verifies SCTs on every HTTPS request.
It supports all three SCT delivery methods: X.509 embedded, TLS extension, and OCSP stapling.
This works on both **Android** and **JVM Desktop**.

```kotlin
import com.jermey.seal.jvm.okhttp.certificateTransparencyInterceptor
import com.jermey.seal.jvm.okhttp.installCertificateTransparency

val client = OkHttpClient.Builder()
    .installCertificateTransparency {
        +"*.example.com"
        -"internal.example.com"
        policy = ChromeCtPolicy()
        failOnError = true
        logger = { host, result ->
            println("CT: $host: $result")
        }
    }
    .build()
```

**Conscrypt** is required for TLS extension and OCSP SCT access. Initialize it early:

```kotlin
import com.jermey.seal.jvm.ConscryptInitializer

// Android: in Application.onCreate()
// JVM Desktop: at program start
ConscryptInitializer.initialize()
```

For full guide, see [OkHttp Integration](https://jermeyyy.github.io/seal/guides/okhttp).

## TrustManager Integration (Android & JVM Desktop)

For apps that need CT verification at the TLS level (instead of the OkHttp interceptor level):

```kotlin
import com.jermey.seal.jvm.trust.CTTrustManagerFactory

val ctTrustManager = CTTrustManagerFactory.create {
    +"*.example.com"
    failOnError = true
}

// Use with SSLContext, OkHttp, or any TLS library
val sslContext = SSLContext.getInstance("TLS")
sslContext.init(null, arrayOf(ctTrustManager), null)
```

## Configuration Reference

Both the OkHttp interceptor and the Ktor plugin share the same DSL (`CTConfigurationBuilder`):

```kotlin
certificateTransparencyInterceptor {
    // ── Host Filtering ──────────────────────────────────────────────
    +"*.example.com"          // include hosts matching pattern
    -"internal.example.com"   // exclude specific hosts (takes precedence)
    // If no includes are specified, all hosts are checked.

    // ── Policy ──────────────────────────────────────────────────────
    policy = ChromeCtPolicy()   // or AppleCtPolicy(), or a custom CTPolicy

    // ── Failure Mode ────────────────────────────────────────────────
    failOnError = false         // false = fail-open (default), true = fail-closed

    // ── Logging ─────────────────────────────────────────────────────
    logger = { host, result ->
        println("CT: $host -> $result")
    }

    // ── Log List ────────────────────────────────────────────────────
    logListUrl = "https://www.gstatic.com/ct/log_list/v3/log_list.json"
    logListCache = myDiskCache   // optional: provide a custom cache
    logListMaxAge = 24.hours     // how long to consider a cached list valid
}
```

### Host Patterns

| Pattern | Matches | Does NOT Match |
|---------|---------|----------------|
| `+"*.example.com"` | `api.example.com`, `www.example.com` | `example.com` |
| `+"**.example.com"` | `deep.sub.example.com` | `example.com` |
| No includes specified | All hosts | — |
| `-"internal.example.com"` | Excluded (even if another pattern includes it) | — |

Excludes always take precedence over includes.

For full reference, see [Configuration](https://jermeyyy.github.io/seal/guides/configuration).

## CT Policy Selection

By default, Seal uses `ChromeCtPolicy`, which requires SCTs from at least one Google-operated
log and one non-Google log. This mirrors Chrome's behavior but can be too strict for some
certificates.

If you encounter "Too few distinct operators" failures, you can switch to Apple's policy:

```kotlin
// Kotlin DSL
installCertificateTransparency {
    useApplePolicy() // or: policy = AppleCtPolicy()
}
```

| Policy | SCT Count (< 180 days) | SCT Count (≥ 180 days) | Operator Diversity |
|--------|----------------------|----------------------|-------------------|
| `ChromeCtPolicy` | ≥ 2 | ≥ 3 | 1 Google + 1 non-Google |
| `AppleCtPolicy` | ≥ 2 | ≥ 3 | 2+ distinct (any) |

## Custom Policies

Seal ships with two built-in CT policies:

| Policy | Description |
|--------|-------------|
| `ChromeCtPolicy()` | Mirrors Chrome's CT requirements — SCT count depends on certificate lifetime (default) |
| `AppleCtPolicy()` | Mirrors Apple's CT requirements — stricter operator diversity rules |

You can implement a custom policy by implementing the `CTPolicy` interface:

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

For more details, see [Configuration](https://jermeyyy.github.io/seal/guides/configuration).

## iOS

### SecTrust Integration

`IosCertificateTransparencyVerifier` combines manual embedded-SCT verification with OS-level TLS/OCSP CT checking:

```kotlin
import com.jermey.seal.ios.IosCertificateTransparencyVerifier

val configuration = ctConfiguration {
    failOnError = true
}
val verifier = IosCertificateTransparencyVerifier(configuration)
val result = verifier.verify(secTrust, host)
```

### URLSession Delegate

`UrlSessionCtHelper` wraps CT verification for use in a `URLSessionDelegate`:

```kotlin
import com.jermey.seal.ios.urlsession.UrlSessionCtHelper

val helper = UrlSessionCtHelper(configuration, verifier)

// In your URLSessionDelegate:
override fun URLSession(
    session: NSURLSession,
    didReceiveChallenge: NSURLAuthenticationChallenge,
    completionHandler: (NSURLSessionAuthChallengeDisposition, NSURLCredential?) -> Unit,
) {
    val (disposition, credential) = helper.handleServerTrustChallenge(didReceiveChallenge)
    completionHandler(disposition, credential)
}
```

### Ktor Darwin Engine

For Ktor-based iOS apps, use the `CertificateTransparency` plugin with the Darwin engine — no additional iOS-specific setup required.

For full guide, see [iOS Integration](https://jermeyyy.github.io/seal/guides/ios).

## Verification Results

Seal reports results through a sealed class hierarchy:

```kotlin
sealed class VerificationResult {
    sealed class Success : VerificationResult() {
        data class Trusted(val validScts: List<SctVerificationResult.Valid>)
        data object InsecureConnection      // HTTP — no TLS
        data object DisabledForHost         // host excluded by matcher
        data object DisabledStaleLogList    // stale log list, policy allows skip
    }
    sealed class Failure : VerificationResult() {
        data object NoScts                  // no SCTs found
        data class TooFewSctsTrusted(val found: Int, val required: Int)
        data class TooFewDistinctOperators(val found: Int, val required: Int)
        data class LogServersFailed(val sctResults: List<SctVerificationResult>)
        data class UnknownError(val cause: Throwable)
    }
}
```

## Platform Notes

### Android

| | |
|---|---|
| **Min SDK** | 24 (Android 7.0) |
| **SCT Methods** | Embedded, TLS extension, OCSP stapling (all three) |
| **Conscrypt** | Required for TLS extension and OCSP SCT access |

### iOS

| | |
|---|---|
| **Min Target** | iOS 15+ |
| **Embedded SCTs** | Verified manually by Seal against its bundled log list |
| **TLS / OCSP SCTs** | Delegated to OS via `SecTrustCopyResult` CT compliance flag |

### JVM Desktop

| | |
|---|---|
| **JVM Target** | 11+ |
| **SCT Methods** | Embedded, TLS extension, OCSP stapling (all three) |
| **Conscrypt** | Required (bundled via `conscrypt-openjdk-uber`) |
| **Disk Cache** | `JvmDiskLogListCache` — defaults to `~/.seal/cache` |

### Web (wasmJs)

| | |
|---|---|
| **Runtime** | Modern browsers with WebAssembly support |
| **CT Verification** | Handled natively by the browser — Seal provides audit-mode reporting |
| **Note** | Browsers enforce their own CT policies; Seal's verification is informational only |

## API Documentation

Full KDoc API documentation is available at: **[jermeyyy.github.io/seal/api/](https://jermeyyy.github.io/seal/api/)**

## Demo App

The repository includes a Compose Multiplatform demo app targeting Android, iOS, JVM Desktop, and Web.

```shell
# Android
./gradlew :composeApp:assembleDebug

# JVM Desktop
./gradlew :composeApp:run

# Web (wasmJs)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# iOS — open in Xcode
open iosApp/iosApp.xcodeproj
```

The demo app makes HTTPS requests and displays CT verification results in real time.

## FAQ / Troubleshooting

<details>
<summary><strong>Why must it be a network interceptor?</strong></summary>

OkHttp network interceptors run *after* the TLS handshake, which means the certificate chain is available for inspection.
Application interceptors run before the connection is established and cannot access TLS state.

</details>

<details>
<summary><strong>What happens when CT verification fails?</strong></summary>

By default (`failOnError = false`), failures are reported through the `logger` callback but the connection proceeds normally (fail-open).
If you set `failOnError = true`, a `SSLPeerUnverifiedException` is thrown and the connection is aborted.

</details>

<details>
<summary><strong>Do I need Conscrypt?</strong></summary>

On **Android** and **JVM Desktop**, Conscrypt is required to access TLS extension and OCSP-stapled SCTs. Without it, only embedded X.509 SCTs are verified.
Call `ConscryptInitializer.initialize()` as early as possible.

On **iOS** and **Web**, Conscrypt is not used.

</details>

<details>
<summary><strong>What if the log list is stale or unavailable?</strong></summary>

Seal ships a bundled log list compiled at build time. If the network fetch fails or the cached list is stale, the bundled list is used as a fallback.
When `failOnError = false` (default), a stale log list will not block connections.

</details>

<details>
<summary><strong>Does this library pin certificates?</strong></summary>

No. Seal performs Certificate Transparency verification, not certificate pinning. CT checks that certificates were publicly logged;
pinning restricts which certificates are accepted. They are complementary techniques.

</details>

<details>
<summary><strong>Does the web target actually verify CT?</strong></summary>

No. In browsers, Certificate Transparency is enforced natively by the browser engine.
The wasmJs target provides the same API surface for code sharing but does not perform additional CT verification — the browser already handles it.

</details>

## Building

```shell
# Build all modules
./gradlew assemble

# Run all tests
./gradlew allTests

# Build the demo app (Android)
./gradlew :composeApp:assembleDebug

# Run the demo app (Desktop)
./gradlew :composeApp:run

# Run the demo app (Web)
./gradlew :composeApp:wasmJsBrowserDevelopmentRun

# Generate API documentation
./gradlew dokkaGenerateModuleHtml
```

For the iOS demo app, open `iosApp/iosApp.xcodeproj` in Xcode and run from there.

## Contributing

Contributions are welcome! Please open an issue first to discuss what you'd like to change.

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/my-feature`)
3. Commit your changes (`git commit -am 'Add my feature'`)
4. Push to the branch (`git push origin feature/my-feature`)
5. Open a Pull Request

Please ensure all tests pass (`./gradlew allTests`) and follow the existing code style.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

[Documentation](https://jermeyyy.github.io/seal/) · [API Reference](https://jermeyyy.github.io/seal/api/) · [Certificate Transparency](https://certificate.transparency.dev/) · [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)
