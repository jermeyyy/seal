# Seal — Project Structure

```
seal/                              # Root project (name: "Seal")
├── build.gradle.kts               # Root build — declares plugins (apply false)
├── settings.gradle.kts            # Module registration, repositories
├── gradle.properties              # JVM args, kotlin.code.style=official, caching
├── gradle/
│   └── libs.versions.toml         # Version catalog (Kotlin 2.3.0, Compose 1.10.0, AGP 8.11.2)
├── gradlew / gradlew.bat          # Gradle wrapper
├── README.md                      # Full library documentation
├── plans/
│   └── seal-implementation-plan.md  # 7-phase implementation plan
├── .github/
│   └── agents/                    # GitHub Copilot agent definitions
│       ├── Architect.agent.md
│       ├── Simple-Architect.agent.md
│       ├── Developer.agent.md
│       └── Simple-Developer.agent.md
├── art/                           # Logo and assets
├── iosApp/                        # iOS Xcode project
│   ├── iosApp.xcodeproj/
│   ├── iosApp/
│   │   ├── ContentView.swift
│   │   ├── iOSApp.swift
│   │   └── Info.plist
│   └── Configuration/
│       └── Config.xcconfig
├── composeApp/                    # Demo/test application (KMP)
│   ├── build.gradle.kts           # Android app + iOS framework
│   └── src/
│       ├── commonMain/kotlin/com/jermey/seal/
│       │   ├── App.kt             # Compose UI entry point
│       │   ├── Platform.kt        # expect fun getPlatform()
│       │   └── Greeting.kt        # Simple greeting class
│       ├── androidMain/kotlin/com/jermey/seal/
│       │   ├── MainActivity.kt
│       │   └── Platform.android.kt
│       ├── iosMain/kotlin/com/jermey/seal/
│       │   ├── MainViewController.kt
│       │   └── Platform.ios.kt
│       └── commonTest/kotlin/com/jermey/seal/
│           └── ComposeAppCommonTest.kt
│
│   --- Planned modules (not yet created) ---
├── seal-core/                     # KMP library: models, ASN.1, SCT parsing, policy
├── seal-android/                  # Android library: Conscrypt, OkHttp, TrustManager
├── seal-ios/                      # KMP library (iOS): SecTrust, URLSession
└── seal-ktor/                     # KMP library: Ktor HttpClient plugin
```

## Notes
- Currently only `composeApp` module exists with template code
- Library modules (`seal-core`, `seal-android`, `seal-ios`, `seal-ktor`) are planned but not yet created
- Phase 0 of the implementation plan covers project restructuring
