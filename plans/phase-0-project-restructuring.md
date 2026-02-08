# Phase 0: Project Restructuring

> **Prerequisites**: None — this is the first phase
> **Summary**: Convert the single-module Compose Multiplatform template into a multi-module library project with publishing support. Creates the directory skeleton, Gradle build files, version catalog entries, and Maven Central publishing configuration for all four library modules.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Target Module Structure (Quick Reference)

| Module          | Type       | Targets                            | Purpose                                    |
|-----------------|------------|------------------------------------|--------------------------------------------|
| `:seal-core`    | KMP lib    | Android, iosArm64, iosSimulatorArm64 | Data models, parsing, verification, policy |
| `:seal-android` | Android lib| Android only                       | Conscrypt, OkHttp, TrustManager            |
| `:seal-ios`     | KMP lib    | iosArm64, iosSimulatorArm64       | SecTrust, URLSession helpers               |
| `:seal-ktor`    | KMP lib    | Android, iosArm64, iosSimulatorArm64 | Ktor plugin wrapping platform logic        |
| `:composeApp`   | KMP app    | Android, iosArm64, iosSimulatorArm64 | Demo / test app                            |

---

## Phase Dependencies

This phase has no external phase dependencies. It must complete before Phases 1-7.

---

## Dependency Graph (Phase 0)

```
0.1  Module directories
0.2  settings.gradle.kts          ← 0.1
0.3  Version catalog
0.4  seal-core build.gradle.kts   ← 0.2, 0.3
0.5  seal-android build.gradle    ← 0.2, 0.3, 0.4
0.6  seal-ios build.gradle        ← 0.2, 0.3, 0.4
0.7  seal-ktor build.gradle       ← 0.2, 0.3, 0.5, 0.6
0.8  composeApp deps              ← 0.4, 0.5, 0.6, 0.7
0.9  Publishing convention        ← 0.4..0.7
0.10 .gitignore                   ← 0.1
```

---

## Tasks

---

### Task 0.1: Create Module Directory Structure

**Description**: Create the directory skeleton for all four library modules with KMP source set directories.

**Files to create**:
```
seal-core/
  src/
    commonMain/kotlin/com/jermey/seal/core/
    commonTest/kotlin/com/jermey/seal/core/
    androidMain/kotlin/com/jermey/seal/core/
    iosMain/kotlin/com/jermey/seal/core/
seal-android/
  src/
    main/kotlin/com/jermey/seal/android/
    test/kotlin/com/jermey/seal/android/
seal-ios/
  src/
    iosMain/kotlin/com/jermey/seal/ios/
    iosArm64Main/kotlin/
    iosSimulatorArm64Main/kotlin/
seal-ktor/
  src/
    commonMain/kotlin/com/jermey/seal/ktor/
    androidMain/kotlin/com/jermey/seal/ktor/
    iosMain/kotlin/com/jermey/seal/ktor/
```

**Dependencies**: None
**Acceptance Criteria**: Directories exist; Gradle sync recognizes all modules
**Complexity**: Low

---

### Task 0.2: Update settings.gradle.kts

**Description**: Register all new modules in the root settings file.

**Files to modify**:
- `settings.gradle.kts`

**Changes**:
```kotlin
include(":composeApp")
include(":seal-core")
include(":seal-android")
include(":seal-ios")
include(":seal-ktor")
```

**Dependencies**: 0.1
**Acceptance Criteria**: `./gradlew projects` lists all five modules
**Complexity**: Low

---

### Task 0.3: Update Version Catalog with New Dependencies

**Description**: Add all required dependency versions and library aliases to `gradle/libs.versions.toml`.

**Files to modify**:
- `gradle/libs.versions.toml`

**New entries (versions)**:
```toml
kotlinx-serialization = "1.8.1"
kotlinx-datetime = "0.6.2"
kotlinx-io = "0.7.0"
kotlinx-coroutines = "1.10.2"
ktor = "3.1.3"
okhttp = "4.12.0"
conscrypt = "2.5.2"
```

**New entries (libraries)**:
```toml
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "kotlinx-serialization" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "kotlinx-datetime" }
kotlinx-io-core = { module = "org.jetbrains.kotlinx:kotlinx-io-core", version.ref = "kotlinx-io" }
kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "kotlinx-coroutines" }
kotlinx-coroutines-test = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "kotlinx-coroutines" }
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-okhttp = { module = "io.ktor:ktor-client-okhttp", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-client-mock = { module = "io.ktor:ktor-client-mock", version.ref = "ktor" }
okhttp = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-mockwebserver = { module = "com.squareup.okhttp3:mockwebserver", version.ref = "okhttp" }
conscrypt-android = { module = "org.conscrypt:conscrypt-android", version.ref = "conscrypt" }
```

**New entries (plugins)**:
```toml
kotlinx-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
maven-publish = { id = "maven-publish" }
```

**Dependencies**: None
**Acceptance Criteria**: `./gradlew --refresh-dependencies` resolves all new aliases
**Complexity**: Low

---

### Task 0.4: Create seal-core build.gradle.kts

**Description**: Configure the `:seal-core` module as a KMP library targeting Android + iOS with `kotlinx-serialization`, `kotlinx-datetime`, and `kotlinx-io` dependencies. Enable `explicitApi()`. Configure `maven-publish`.

**Files to create**:
- `seal-core/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinx.serialization)
    `maven-publish`
}

kotlin {
    explicitApi()

    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.io.core)
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

android {
    namespace = "com.jermey.seal.core"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

group = "io.github.jermeyyy.seal"
version = "0.1.0"

publishing {
    // Maven Central configuration (GPG signing, POM metadata)
}
```

**Dependencies**: 0.2, 0.3
**Acceptance Criteria**: `./gradlew :seal-core:assemble` compiles successfully, produces AAR and iOS framework
**Complexity**: Medium

---

### Task 0.5: Create seal-android build.gradle.kts

**Description**: Configure `:seal-android` as an Android-only library module with Conscrypt and OkHttp dependencies. Depends on `:seal-core`.

**Files to create**:
- `seal-android/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    explicitApi()
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }

    sourceSets {
        androidMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.conscrypt.android)
            implementation(libs.okhttp)
            implementation(libs.kotlinx.coroutines.android)
        }
    }
}

android {
    namespace = "com.jermey.seal.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

group = "io.github.jermeyyy.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.4
**Acceptance Criteria**: `./gradlew :seal-android:assembleDebug` succeeds with Conscrypt and OkHttp on classpath
**Complexity**: Medium

---

### Task 0.6: Create seal-ios build.gradle.kts

**Description**: Configure `:seal-ios` as a KMP library targeting iOS only. Depends on `:seal-core`.

**Files to create**:
- `seal-ios/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    `maven-publish`
}

kotlin {
    explicitApi()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
        }
    }
}

group = "io.github.jermeyyy.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.4
**Acceptance Criteria**: `./gradlew :seal-ios:compileKotlinIosArm64` succeeds
**Complexity**: Low

---

### Task 0.7: Create seal-ktor build.gradle.kts

**Description**: Configure `:seal-ktor` as a KMP library with Ktor client dependency. Platform source sets depend on `:seal-android` (Android) and `:seal-ios` (iOS).

**Files to create**:
- `seal-ktor/build.gradle.kts`

**Key configuration**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    explicitApi()
    androidTarget { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(project(":seal-android"))
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(project(":seal-ios"))
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "com.jermey.seal.ktor"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    defaultConfig { minSdk = libs.versions.android.minSdk.get().toInt() }
}

group = "io.github.jermeyyy.seal"
version = "0.1.0"
```

**Dependencies**: 0.2, 0.3, 0.5, 0.6
**Acceptance Criteria**: `./gradlew :seal-ktor:assemble` succeeds
**Complexity**: Medium

---

### Task 0.8: Update composeApp to Depend on Library Modules

**Description**: Add library module dependencies to `composeApp/build.gradle.kts` so the demo app can exercise the library.

**Files to modify**:
- `composeApp/build.gradle.kts`

**Changes**: Add in `sourceSets`:
```kotlin
commonMain.dependencies {
    implementation(project(":seal-core"))
    implementation(project(":seal-ktor"))
}
androidMain.dependencies {
    implementation(project(":seal-android"))
}
iosMain.dependencies {
    implementation(project(":seal-ios"))
}
```

**Dependencies**: 0.4, 0.5, 0.6, 0.7
**Acceptance Criteria**: `./gradlew :composeApp:assembleDebug` succeeds with library modules on classpath
**Complexity**: Low

---

### Task 0.9: Configure Shared Publishing Convention Plugin

**Description**: Create a convention plugin (or `buildSrc`/included build) to share POM metadata, signing configuration, and Maven Central publishing setup across all library modules. Configure group ID `io.github.jermeyyy.seal`.

**Files to create**:
- `build-logic/convention/build.gradle.kts`
- `build-logic/convention/src/main/kotlin/seal.publishing.gradle.kts`
- `build-logic/settings.gradle.kts`

Or alternatively, configure directly in root `build.gradle.kts` via `subprojects`/`allprojects`.

**Key bits**:
- Group ID: `io.github.jermeyyy.seal`
- Artifact IDs: `seal-core`, `seal-android`, `seal-ios`, `seal-ktor`
- POM: name, description, URL, licenses (Apache 2.0), developers, SCM
- Signing: GPG key configuration via environment variables
- Repository: Sonatype OSSRH / Maven Central staging

**Dependencies**: 0.4, 0.5, 0.6, 0.7
**Acceptance Criteria**: `./gradlew publishToMavenLocal` produces artifacts under `io/github/jermey/seal/` in `~/.m2`
**Complexity**: High

---

### Task 0.10: Add .gitignore Entries for New Modules

**Description**: Ensure new module `build/` directories and generated files are properly gitignored.

**Files to modify**:
- `.gitignore` (root, create if missing)

**Dependencies**: 0.1
**Acceptance Criteria**: `git status` does not show generated files
**Complexity**: Low
