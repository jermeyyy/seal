# Seal — Project Structure

```
seal/                              # Root project (name: "Seal")
├── build.gradle.kts               # Root build — declares plugins (apply false), Dokka aggregation
├── settings.gradle.kts            # Module registration, repositories, TYPESAFE_PROJECT_ACCESSORS
├── gradle.properties              # JVM args, kotlin.code.style=official, caching
├── gradle/
│   └── libs.versions.toml         # Version catalog (Kotlin 2.3.0, Compose 1.10.0, AGP 9.0.0)
├── gradlew / gradlew.bat          # Gradle wrapper
├── README.md                      # Full production-quality library documentation
├── CHANGELOG.md                   # Changelog (v0.1.0 initial release)
├── plans/                         # Implementation plans (all phases completed)
│   ├── seal-implementation-plan.md
│   ├── 00-architecture-overview.md
│   ├── phase-0 through phase-8 docs
│   └── analysis-sct-verification-mismatch.md / fix-sct-verification-mismatch.md
├── build-logic/
│   ├── settings.gradle.kts
│   ├── build.gradle.kts
│   └── convention/
│       ├── build.gradle.kts       # kotlin-dsl, vanniktech maven publish 0.34.0
│       └── src/main/kotlin/seal.publishing.gradle.kts  # Convention plugin for Maven Central
├── .github/
│   └── agents/                    # GitHub Copilot agent definitions
│       ├── Architect.agent.md
│       ├── Simple-Architect.agent.md
│       ├── Developer.agent.md
│       └── Simple-Developer.agent.md
├── art/                           # Logo and assets
│
├── seal-core/                     # KMP library: models, ASN.1, SCT parsing, X.509, verification, policy, platform integrations
│   ├── build.gradle.kts           # KMP (android + iOS + jvm + wasmJs), explicitApi()
│   └── src/
│       ├── commonMain/kotlin/com/jermey/seal/core/
│       │   ├── model/             # SCT, VerificationResult, LogServer, LogId, etc.
│       │   ├── asn1/              # Asn1Parser, Asn1Element, Asn1Tag, Oid
│       │   ├── parser/            # SctListParser, SctDeserializer
│       │   ├── x509/              # CertificateParser, ParsedCertificate, TbsCertificateBuilder
│       │   ├── crypto/            # CryptoVerifier interface, createCryptoVerifier() expect
│       │   ├── verification/      # CertificateTransparencyVerifier, SctSignatureVerifier
│       │   ├── policy/            # CTPolicy, ChromeCtPolicy, AppleCtPolicy
│       │   ├── config/            # CTConfiguration, CTConfigurationBuilder, ctConfiguration {}
│       │   ├── host/              # HostPattern, HostMatcher
│       │   └── loglist/           # LogListService, LogListParser, LogListMapper, EmbeddedLogListData, DTOs, cache
│       ├── jvmSharedMain/kotlin/com/jermey/seal/jvm/
│       │   ├── okhttp/            # CertificateTransparencyInterceptor, ConscryptCtSocketFactory
│       │   ├── conscrypt/         # ConscryptSctExtractor, OcspResponseParser
│       │   ├── chain/             # CertificateChainCleaner
│       │   └── trust/             # CTTrustManager, CTTrustManagerFactory
│       ├── androidMain/kotlin/com/jermey/seal/core/
│       │   ├── crypto/            # JCE-based CryptoVerifier
│       │   └── loglist/           # Android resource loading, disk cache
│       ├── jvmMain/kotlin/com/jermey/seal/core/
│       │   └── loglist/           # JVM disk cache, resource loading
│       ├── iosMain/kotlin/com/jermey/seal/
│       │   ├── ios/               # IosCertificateTransparencyVerifier, SecTrust, URLSession helpers
│       │   ├── core/crypto/       # Security framework CryptoVerifier
│       │   └── core/loglist/      # NSBundle resource loading, iOS disk cache
│       ├── wasmJsMain/            # Browser integration (audit mode only)
│       ├── commonTest/            # 12 test classes
│       └── jvmSharedTest/         # OkHttp interceptor integration tests
│
├── seal-ktor/                     # KMP library: Ktor HttpClient plugin
│   ├── build.gradle.kts           # KMP (android + iOS + jvm + wasmJs), explicitApi()
│   └── src/
│       ├── commonMain/            # CertificateTransparency plugin, installPlatformCt() expect
│       ├── androidMain/           # installPlatformCt() actual (OkHttp)
│       ├── jvmMain/               # installPlatformCt() actual (OkHttp)
│       ├── iosMain/               # installPlatformCt() actual (Darwin)
│       └── wasmJsMain/            # installPlatformCt() actual (browser-native)
│
├── composeApp/                    # Demo app — shared Compose Multiplatform UI
│   ├── build.gradle.kts           # KMP + Compose + Serialization
│   └── src/
│       ├── commonMain/kotlin/com/jermey/seal/
│       │   ├── App.kt             # Root composable
│       │   └── demo/
│       │       ├── data/          # CtVerificationRepository, DTOs
│       │       ├── mvi/           # FlowMVI: HomeContract/Container, DetailsContract/Container
│       │       ├── ui/            # HomeScreen, DetailsScreen, components/
│       │       ├── di/            # Koin AppModule
│       │       ├── navigation/    # Quo Vadis navigation config
│       │       ├── CtCheckResult.kt, SealTheme.kt, PlatformInfo.kt, HttpEngineFactory.kt
│       │       └── Engine.kt
│       ├── androidMain/           # Platform actuals for demo
│       ├── iosMain/               # Platform actuals + MainViewController
│       ├── desktopMain/           # JVM Desktop platform actuals
│       ├── wasmJsMain/            # Web platform actuals
│       └── commonTest/            # ComposeAppCommonTest
│
├── androidApp/                    # Android host app for demo
│   ├── build.gradle.kts           # Android application module
│   └── src/main/
│       ├── kotlin/com/jermey/seal/
│       │   ├── MainActivity.kt
│       │   └── SealApplication.kt
│       ├── AndroidManifest.xml
│       └── res/                   # Launcher icons, resources
│
├── docs/
│   └── site/                      # React/TypeScript documentation website (Vite)
│       ├── package.json
│       ├── vite.config.ts
│       └── src/
│           └── pages/             # Home, GettingStarted, WhyCT, Guides (OkHttp, Ktor, iOS, Config, CustomPolicies), Demo
│
└── commonMain/                    # ⚠️ Stale/orphaned directory (not referenced by any build file)
    └── com/jermey/
```

## Test Coverage Summary
| Module | Tests | Coverage |
|--------|-------|----------|
| seal-core | 12 test classes (commonTest) + jvmSharedTest | ASN.1, OID, host matching, log list, config, SCT parsing, X.509, verification, policy, OkHttp interceptor |
| seal-ktor | 0 | No tests |
| composeApp | 1 test class (commonTest) | Basic smoke test |
