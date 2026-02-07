# Seal — AGP 9 / Gradle 9 Migration Plan

> **Version**: 1.0  
> **Created**: 2026-02-07  
> **Status**: Draft  
> **Applies to**: All modules (current and future)  
> **Reference**: [KotlinLang AGP 9 Migration Guide](https://kotlinlang.org/docs/multiplatform/multiplatform-project-agp-9-migration.html)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current vs Target State](#current-vs-target-state)
3. [Version Compatibility Matrix](#version-compatibility-matrix)
4. [Migration Phases](#migration-phases)
   - [Phase 1: Create androidApp Module](#phase-1-create-androidapp-module)
   - [Phase 2: Reconfigure composeApp](#phase-2-reconfigure-composeapp)
   - [Phase 3: Update Version Catalog](#phase-3-update-version-catalog)
   - [Phase 4: Update Gradle Wrapper](#phase-4-update-gradle-wrapper)
   - [Phase 5: Update Root build.gradle.kts](#phase-5-update-root-buildgradlekts)
   - [Phase 6: Update settings.gradle.kts](#phase-6-update-settingsgradlekts)
   - [Phase 7: Cleanup & Verification](#phase-7-cleanup--verification)
   - [Phase 8: Future Module Guidance](#phase-8-future-module-guidance)
5. [Risks & Mitigations](#risks--mitigations)
6. [Rollback Strategy](#rollback-strategy)

---

## Executive Summary

AGP 9.0 removes compatibility between `com.android.application` / `com.android.library` and `org.jetbrains.kotlin.multiplatform`. This migration:

1. **Extracts** the Android entry point (`MainActivity`, `AndroidManifest.xml`) into a new `:androidApp` module using the standard `com.android.application` plugin
2. **Converts** `:composeApp` from an Android application module into a KMP shared library using the new `com.android.kotlin.multiplatform.library` plugin
3. **Upgrades** AGP 8.11.2 → 9.0.0 and Gradle 8.14.3 → 9.1.0
4. **Validates** that Kotlin 2.3.0 and Compose Multiplatform 1.10.0 are compatible (they are — no version bumps needed)

All changes are designed as a **single-pass migration**: apply every change, then sync and build once.

---

## Current vs Target State

### Current Module Structure

```
Seal/
├── composeApp/          ← KMP app module (androidApplication + kotlinMultiplatform)
│   ├── src/androidMain/ ← MainActivity, Platform.android.kt, AndroidManifest.xml
│   ├── src/commonMain/  ← App.kt, Platform.kt, Greeting.kt
│   ├── src/iosMain/     ← MainViewController.kt, Platform.ios.kt
│   └── src/commonTest/
└── iosApp/              ← Xcode project
```

### Target Module Structure

```
Seal/
├── androidApp/          ← NEW: Pure Android app module (androidApplication only)
│   └── src/main/        ← MainActivity.kt, AndroidManifest.xml
├── composeApp/          ← KMP shared library (kotlinMultiplatform + androidMultiplatformLibrary)
│   ├── src/androidMain/ ← Platform.android.kt (actual declarations only)
│   ├── src/commonMain/  ← App.kt, Platform.kt, Greeting.kt
│   ├── src/iosMain/     ← MainViewController.kt, Platform.ios.kt
│   └── src/commonTest/
└── iosApp/              ← Xcode project (unchanged)
```

### Key Principle

| Aspect | What stays in `composeApp` | What moves to `androidApp` |
|--------|---------------------------|---------------------------|
| Compose UI (`App.kt`) | ✅ Shared across platforms | |
| `expect`/`actual` declarations | ✅ Must stay for all platforms | |
| `MainActivity.kt` | | ✅ Android entry point |
| `AndroidManifest.xml` | Stripped to library manifest | ✅ App manifest with `<activity>` |
| Application ID, version | | ✅ App-level config |
| Android deps (activity-compose) | | ✅ Moved to androidApp |

---

## Version Compatibility Matrix

| Component | Current | Target | Notes |
|-----------|---------|--------|-------|
| AGP | 8.11.2 | **9.0.0** | Required for `com.android.kotlin.multiplatform.library` |
| Gradle | 8.14.3 | **9.1.0** | Minimum for AGP 9.0.0 |
| Kotlin | 2.3.0 | 2.3.0 | Compatible with AGP 9.0.0 — no change needed |
| Compose Multiplatform | 1.10.0 | 1.10.0 | Compatible with Kotlin 2.3.0 and AGP 9.0.0 — no change needed |
| Android compileSdk | 36 | 36 | No change needed |
| Android minSdk | 24 | 24 | No change needed |
| JVM target | 11 | 11 | No change needed |

> **Note**: Kotlin 2.3.0 + Compose MP 1.10.0 + AGP 9.0.0 is the exact combination used in
> the [official reference project](https://github.com/kotlin-hands-on/get-started-with-cm/tree/new-project-structure).
> No version upgrades beyond AGP and Gradle are required.

---

## Migration Phases

> **Execution order**: All phases are applied in a single pass. The ordering below is the
> logical dependency order — apply them top-to-bottom, then sync once.

---

### Phase 1: Create androidApp Module

**Goal**: Create a new `:androidApp` module that contains only the Android app entry point.

#### Task 1.1: Create androidApp Directory Structure

**Description**: Create the `androidApp/` directory with `src/main/` layout.

**Files to create**:
- `androidApp/` (directory)
- `androidApp/src/main/kotlin/com/jermey/seal/` (directory)

**Dependencies**: None  
**Acceptance criteria**: Directories exist

---

#### Task 1.2: Move MainActivity.kt

**Description**: Move the Android entry point from `composeApp/src/androidMain/` to `androidApp/src/main/`. The file content stays the same — it imports `App()` from the shared module.

**Source**: `composeApp/src/androidMain/kotlin/com/jermey/seal/MainActivity.kt`  
**Destination**: `androidApp/src/main/kotlin/com/jermey/seal/MainActivity.kt`

**Content** (unchanged):
```kotlin
package com.jermey.seal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}
```

> **Important**: `App()` is defined in `composeApp/src/commonMain/` and will be available via
> the `implementation(projects.composeApp)` dependency.

**Dependencies**: 1.1  
**Acceptance criteria**: File exists at new location; old location will be deleted in Phase 7

---

#### Task 1.3: Create AndroidManifest.xml for androidApp

**Description**: Create the app-level Android manifest in `androidApp`. This is the manifest that declares the application, activity, theme, etc.

**File to create**: `androidApp/src/main/AndroidManifest.xml`

**Content**:
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:allowBackup="true"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@android:style/Theme.Material.Light.NoActionBar">
        <activity
            android:name=".MainActivity"
            android:configChanges="orientation|screenSize|screenLayout|keyboardHidden|mnc|colorMode|density|fontScale|fontWeightAdjustment|keyboard|layoutDirection|locale|mcc|navigation|smallestScreenSize|touchscreen|uiMode"
            android:exported="true">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

> **Note**: If the current `composeApp/src/androidMain/AndroidManifest.xml` has specific
> activity/application declarations, copy them here. The `composeApp` manifest will be
> stripped down to a minimal library manifest (or removed entirely since `androidLibrary {}`
> handles the namespace).

**Dependencies**: 1.1  
**Acceptance criteria**: Manifest declares the launcher activity

---

#### Task 1.4: Move Android Resources to androidApp

**Description**: Move app-level Android resources (launcher icons, strings, drawables) from `composeApp/src/androidMain/res/` to `androidApp/src/main/res/`.

**Source**: `composeApp/src/androidMain/res/` (entire directory)  
**Destination**: `androidApp/src/main/res/`

**Contents to move**:
- `drawable/ic_launcher_background.xml`
- `drawable-v24/ic_launcher_foreground.xml`
- `mipmap-*/` (all density directories)
- `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`
- `values/strings.xml`

**Dependencies**: 1.1  
**Acceptance criteria**: All resource files exist under `androidApp/src/main/res/`

---

#### Task 1.5: Create androidApp/build.gradle.kts

**Description**: Create the build script for the Android app module.

**File to create**: `androidApp/build.gradle.kts`

**Content**:
```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    dependencies {
        implementation(projects.composeApp)
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.uiToolingPreview)
    }
}

android {
    namespace = "com.jermey.seal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jermey.seal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

**Key differences from current composeApp**:
- **No** `kotlinMultiplatform` plugin — this is a pure Android module
- Uses `kotlin { target { } }` block instead of `kotlin { androidTarget { } }` (AGP 9 syntax)
- Dependencies declared inside `kotlin { dependencies { } }` block
- `implementation(projects.composeApp)` — depends on the shared KMP library
- Android-specific deps (`activity-compose`, `uiToolingPreview`) moved here from `composeApp`

> **Note on `kotlin {}` block**: With AGP 9.0, the `com.android.application` plugin includes
> built-in Kotlin support. The `kotlin {}` block is provided by AGP, not by a separate Kotlin
> plugin. The `org.jetbrains.kotlin.android` plugin is **not needed** when using AGP 9.0+.

**Dependencies**: 1.3, 1.4  
**Acceptance criteria**: File compiles after full sync

---

### Phase 2: Reconfigure composeApp

**Goal**: Convert `composeApp` from an Android application to a KMP shared library using the new `com.android.kotlin.multiplatform.library` plugin.

#### Task 2.1: Update composeApp/build.gradle.kts — Plugins

**Description**: Replace the `androidApplication` plugin with `androidMultiplatformLibrary`.

**File to modify**: `composeApp/build.gradle.kts`

**Before**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

**After**:
```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}
```

**Dependencies**: Phase 3 (version catalog must have `androidMultiplatformLibrary` defined)  
**Acceptance criteria**: Plugin reference resolves after sync

---

#### Task 2.2: Replace androidTarget {} with androidLibrary {}

**Description**: Remove the old `androidTarget {}` block and the entire `android {}` block. Replace with the new `androidLibrary {}` block inside `kotlin {}`.

**File to modify**: `composeApp/build.gradle.kts`

**Remove** (the `androidTarget` block):
```kotlin
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
```

**Remove** (the entire top-level `android {}` block):
```kotlin
android {
    namespace = "com.jermey.seal"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.jermey.seal"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}
```

**Add** (inside the `kotlin {}` block, before the iOS targets):
```kotlin
    androidLibrary {
        namespace = "com.jermey.seal.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        androidResources {
            enable = true
        }
    }
```

> **Critical**: The namespace must differ from the app's namespace (`com.jermey.seal`).
> Use `com.jermey.seal.shared` for the library.

**Dependencies**: 2.1  
**Acceptance criteria**: No `android {}` block at top level; `androidLibrary {}` inside `kotlin {}`

---

#### Task 2.3: Remove androidMain.dependencies from composeApp

**Description**: Android-specific dependencies (`activity-compose`, `uiToolingPreview`) have been moved to `androidApp`. Remove them from `composeApp`.

**File to modify**: `composeApp/build.gradle.kts`

**Remove from sourceSets**:
```kotlin
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
```

> **Note**: `Platform.android.kt` (the `actual` declaration) stays in `composeApp/src/androidMain/`
> and doesn't need these dependencies. If any future code in `androidMain` needs Android deps,
> add them back selectively.

**Dependencies**: 2.2  
**Acceptance criteria**: No `androidMain.dependencies` block in composeApp

---

#### Task 2.4: Update the top-level dependencies block

**Description**: Replace `debugImplementation` with `androidRuntimeClasspath` since the new Android KMP library plugin doesn't support build variants.

**File to modify**: `composeApp/build.gradle.kts`

**Before**:
```kotlin
dependencies {
    debugImplementation(libs.compose.uiTooling)
}
```

**After**:
```kotlin
dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

**Dependencies**: 2.1  
**Acceptance criteria**: Uses `androidRuntimeClasspath` configuration

---

#### Task 2.5: Remove unused import

**Description**: Remove the `TargetFormat` import that's no longer needed (it was for desktop targets).

**File to modify**: `composeApp/build.gradle.kts`

**Remove**:
```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
```

**Dependencies**: None  
**Acceptance criteria**: No unused imports

---

#### Task 2.6: Delete moved files from composeApp

**Description**: Remove files that have been moved to `androidApp`.

**Files to delete**:
- `composeApp/src/androidMain/kotlin/com/jermey/seal/MainActivity.kt`
- `composeApp/src/androidMain/res/` (entire directory — app resources moved to androidApp)

**Files to keep in composeApp/src/androidMain/**:
- `kotlin/com/jermey/seal/Platform.android.kt` — this is the `actual` declaration, must stay
- `AndroidManifest.xml` — can be removed if the namespace is handled by `androidLibrary {}`, but a minimal one is harmless

> **Note**: The `composeApp/src/androidMain/AndroidManifest.xml` can be simplified to just:
> ```xml
> <?xml version="1.0" encoding="utf-8"?>
> <manifest />
> ```
> Or deleted entirely, since `androidLibrary { namespace = ... }` handles this.

**Dependencies**: 1.2, 1.4  
**Acceptance criteria**: No duplicate `MainActivity.kt`; resources only in `androidApp`

---

#### Task 2.7: Final composeApp/build.gradle.kts (Complete)

**Description**: For reference, here is the complete target state of `composeApp/build.gradle.kts` after all Phase 2 changes.

```kotlin
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.seal.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }

        androidResources {
            enable = true
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    androidRuntimeClasspath(libs.compose.uiTooling)
}
```

---

### Phase 3: Update Version Catalog

**Goal**: Add the new `androidMultiplatformLibrary` plugin, remove the now-unused `androidLibrary` plugin, and update AGP to 9.0.0.

#### Task 3.1: Update gradle/libs.versions.toml

**File to modify**: `gradle/libs.versions.toml`

**Changes to [versions]**:

| Key | Before | After |
|-----|--------|-------|
| `agp` | `"8.11.2"` | `"9.0.0"` |

> Kotlin and Compose versions remain unchanged.

**Changes to [plugins]**:

**Before**:
```toml
[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

**After**:
```toml
[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidMultiplatformLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
```

**Summary of changes**:
1. `agp` version: `8.11.2` → `9.0.0`
2. **Remove**: `androidLibrary` plugin entry (no longer used by any module)
3. **Add**: `androidMultiplatformLibrary` plugin entry

> **Why remove `androidLibrary`?** With AGP 9, `com.android.library` is incompatible with
> `org.jetbrains.kotlin.multiplatform`. The old `androidLibrary` plugin is not used in any
> module after migration. If standalone Android library modules are needed in the future, they
> can use `com.android.library` directly — but they won't be KMP modules.

**Dependencies**: None (can be applied independently, resolves at sync time)  
**Acceptance criteria**: `libs.plugins.androidMultiplatformLibrary` resolves; `libs.plugins.androidLibrary` no longer referenced

---

### Phase 4: Update Gradle Wrapper

**Goal**: Update Gradle from 8.14.3 to 9.1.0 (minimum required for AGP 9.0.0).

#### Task 4.1: Update gradle-wrapper.properties

**File to modify**: `gradle/wrapper/gradle-wrapper.properties`

**Before**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.14.3-bin.zip
```

**After**:
```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-9.1.0-bin.zip
```

> All other lines remain unchanged.

**Dependencies**: None  
**Acceptance criteria**: `./gradlew --version` reports Gradle 9.1.0

---

#### Task 4.2: Update gradle.properties (if needed)

**File to review**: `gradle.properties`

**Current content**:
```properties
org.gradle.jvmargs=-Xmx4096M
org.gradle.caching=true
org.gradle.configuration-cache=true
kotlin.code.style=official
android.nonTransitiveRClass=true
android.useAndroidX=true
kotlin.daemon.jvmargs=-Xmx3072M
```

**Changes**: **None required**. All properties are compatible with Gradle 9.1.0.

> **Do NOT add** `android.enableLegacyVariantApi=true` — that is only a temporary workaround
> to delay migration. We are doing the full migration.

**Acceptance criteria**: No deprecated property warnings during sync

---

### Phase 5: Update Root build.gradle.kts

**Goal**: Declare all plugins used across all modules (with `apply false`) in the root build script.

#### Task 5.1: Update root build.gradle.kts

**File to modify**: `build.gradle.kts` (root)

**Before**:
```kotlin
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
```

**After**:
```kotlin
plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}
```

**Summary of changes**:
1. **Remove**: `alias(libs.plugins.androidLibrary) apply false`
2. **Add**: `alias(libs.plugins.androidMultiplatformLibrary) apply false`

**Dependencies**: Phase 3 (version catalog must be updated first)  
**Acceptance criteria**: Root build script syncs without errors

---

### Phase 6: Update settings.gradle.kts

**Goal**: Register the new `:androidApp` module.

#### Task 6.1: Add androidApp to settings.gradle.kts

**File to modify**: `settings.gradle.kts`

**Before** (last line):
```kotlin
include(":composeApp")
```

**After**:
```kotlin
include(":composeApp")
include(":androidApp")
```

**Dependencies**: Phase 1 (androidApp directory must exist)  
**Acceptance criteria**: `./gradlew projects` lists both `:composeApp` and `:androidApp`

---

### Phase 7: Cleanup & Verification

**Goal**: Remove dead code, verify the build, and ensure everything works.

#### Task 7.1: Delete Moved Files

**Description**: After confirming the build works, delete the original files that were moved to `androidApp`.

**Files to delete**:
- `composeApp/src/androidMain/kotlin/com/jermey/seal/MainActivity.kt`
- `composeApp/src/androidMain/res/` (entire directory tree)

**Files to simplify**:
- `composeApp/src/androidMain/AndroidManifest.xml` → reduce to minimal or delete

> The `composeApp/src/androidMain/AndroidManifest.xml` can be either deleted (the `androidLibrary {}` block
> handles the namespace) or simplified to:
> ```xml
> <?xml version="1.0" encoding="utf-8"?>
> <manifest />
> ```

**Dependencies**: Successful build from all prior phases  
**Acceptance criteria**: No duplicate source files

---

#### Task 7.2: Verify Android Build

**Description**: Run the full Android build pipeline.

**Commands**:
```bash
# Clean and rebuild everything
./gradlew clean

# Sync project
./gradlew :androidApp:dependencies
./gradlew :composeApp:dependencies

# Build Android app
./gradlew :androidApp:assembleDebug

# Run Android unit tests
./gradlew :composeApp:testDebugUnitTest

# Full project build
./gradlew assemble
```

**Expected results**:
- `:androidApp:assembleDebug` produces an APK in `androidApp/build/outputs/apk/debug/`
- `:composeApp` produces an AAR (Android library artifact)
- No compilation errors
- No warnings about deprecated variant API

**Acceptance criteria**: All commands succeed with exit code 0

---

#### Task 7.3: Verify iOS Build

**Description**: Ensure iOS targets still compile correctly.

**Commands**:
```bash
# Compile iOS frameworks
./gradlew :composeApp:compileKotlinIosArm64
./gradlew :composeApp:compileKotlinIosSimulatorArm64

# Link iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

**Expected results**:
- iOS compilation unchanged — the `listOf(iosArm64(), iosSimulatorArm64())` block in `composeApp` is untouched
- Framework output in `composeApp/build/bin/iosSimulatorArm64/debugFramework/`

**Acceptance criteria**: iOS framework links successfully

---

#### Task 7.4: Verify Run Configurations

**Description**: Update IDE run configurations.

**Steps**:
1. Open the project in Android Studio or IntelliJ IDEA
2. Check that a new `androidApp` run configuration appears automatically
3. If the old `composeApp` Android run configuration still exists:
   - Edit it to point to `Seal.androidApp` module instead of `Seal.composeApp`
   - Or delete it and use the auto-generated `androidApp` configuration
4. Run the app on an emulator/device — verify it launches and shows the Compose UI

**Acceptance criteria**: App runs on Android emulator/device

---

### Phase 8: Future Module Guidance

**Goal**: Document how future library modules (`seal-core`, `seal-android`, `seal-ios`, `seal-ktor`) should be configured for AGP 9 compatibility.

---

#### 8.1: KMP Library Modules (seal-core, seal-ktor)

Modules that target both Android and iOS use `com.android.kotlin.multiplatform.library`:

```kotlin
// seal-core/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    // + any other plugins (serialization, etc.)
}

kotlin {
    androidLibrary {
        namespace = "com.jermey.seal.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
    }

    iosArm64()
    iosSimulatorArm64()

    // Dependencies declared with @OptIn or in sourceSets
    sourceSets {
        commonMain.dependencies {
            // ...
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
```

**Key rules for KMP library modules with AGP 9**:
- **Use** `androidMultiplatformLibrary` plugin — NOT `androidLibrary`
- **Use** `kotlin { androidLibrary { } }` block — NOT top-level `android { }`
- **Use** `kotlin { androidLibrary { compilerOptions { } } }` — NOT `androidTarget { compilerOptions { } }`
- **No** `applicationId`, `targetSdk`, `versionCode`, `versionName` — those are app-level
- **No** `buildTypes` or `packaging` — the KMP library plugin doesn't support variants
- **Use** `androidRuntimeClasspath(...)` instead of `debugImplementation(...)` for top-level dependencies

---

#### 8.2: Android-Only Library Module (seal-android)

`seal-android` is Android-only (no iOS target). It can use the standard `com.android.library` plugin
since it does NOT combine with `org.jetbrains.kotlin.multiplatform`:

```kotlin
// seal-android/build.gradle.kts
plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.jermey.seal.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

kotlin {
    target {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
}

dependencies {
    api(project(":seal-core"))
    implementation(libs.conscrypt.android)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)
}
```

> **Important**: If `seal-android` later needs to also be a KMP module (e.g., to share
> some code with a JVM target), it must switch to `androidMultiplatformLibrary` and follow
> the pattern in 8.1.

**Version catalog**: If `seal-android` uses `com.android.library`, re-add the `androidLibrary`
plugin entry to the version catalog:
```toml
androidLibrary = { id = "com.android.library", version.ref = "agp" }
```
And add to root `build.gradle.kts`:
```kotlin
alias(libs.plugins.androidLibrary) apply false
```

---

#### 8.3: iOS-Only Library Module (seal-ios)

`seal-ios` targets only iOS — no Android plugin needed at all:

```kotlin
// seal-ios/build.gradle.kts
plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            api(project(":seal-core"))
        }
    }
}
```

No AGP 9 considerations — this module never interacts with Android Gradle plugins.

---

#### 8.4: Version Catalog Additions for Future Modules

When adding all library modules, the version catalog `[plugins]` section should be:

```toml
[plugins]
androidApplication = { id = "com.android.application", version.ref = "agp" }
androidLibrary = { id = "com.android.library", version.ref = "agp" }
androidMultiplatformLibrary = { id = "com.android.kotlin.multiplatform.library", version.ref = "agp" }
composeMultiplatform = { id = "org.jetbrains.compose", version.ref = "composeMultiplatform" }
composeCompiler = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlinMultiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlinxSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
```

And the root `build.gradle.kts`:
```kotlin
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinxSerialization) apply false
}
```

---

#### 8.5: Module → Plugin Mapping Summary

| Module | Type | Plugin(s) | Uses `android {}` | Uses `androidLibrary {}` |
|--------|------|-----------|-------------------|--------------------------|
| `:androidApp` | Android app | `androidApplication` | ✅ (top-level) | ❌ |
| `:composeApp` | KMP shared lib | `kotlinMultiplatform` + `androidMultiplatformLibrary` | ❌ | ✅ (inside `kotlin {}`) |
| `:seal-core` | KMP lib | `kotlinMultiplatform` + `androidMultiplatformLibrary` | ❌ | ✅ (inside `kotlin {}`) |
| `:seal-android` | Android lib | `androidLibrary` | ✅ (top-level) | ❌ |
| `:seal-ios` | KMP lib (iOS-only) | `kotlinMultiplatform` | ❌ | ❌ |
| `:seal-ktor` | KMP lib | `kotlinMultiplatform` + `androidMultiplatformLibrary` | ❌ | ✅ (inside `kotlin {}`) |

---

## Risks & Mitigations

| # | Risk | Impact | Likelihood | Mitigation |
|---|------|--------|------------|------------|
| R1 | IDE doesn't recognize AGP 9 | Build works but IDE shows red | Medium | Android Studio Otter 3 (2025.2.3)+ supports AGP 9.0. IntelliJ IDEA support expected Q1 2026. Use compatible IDE version. |
| R2 | Compose resources not found after migration | Runtime crash | Low | Enable `androidResources { enable = true }` in `androidLibrary {}` block — already included in plan. |
| R3 | Namespace collision between app and library | Build failure | Low | Library uses `com.jermey.seal.shared`, app uses `com.jermey.seal` — distinct namespaces. |
| R4 | Third-party Gradle plugins incompatible with Gradle 9 | Build failure | Medium | Currently no third-party plugins are used. Evaluate compatibility before adding any. |
| R5 | iOS framework name changes | Xcode build failure | Low | `baseName = "ComposeApp"` and `isStatic = true` are unchanged. iOS project is unaffected by AGP migration. |
| R6 | `androidMain` source set naming changed | Compilation failure | Low | The `androidMultiplatformLibrary` plugin uses the same `androidMain` source set name. No change needed. |
| R7 | Compose UI tooling preview broken in library module | No preview in composeApp | Medium | Preview annotations run in the `androidApp` module which has full Android application context. |

---

## Rollback Strategy

If the migration fails and cannot be resolved quickly:

1. **Git revert**: All changes should be committed in a single commit (or feature branch). Revert the commit.
   ```bash
   git revert <migration-commit-sha>
   ```

2. **Alternative: Legacy bridge** (short-term only): Add to `gradle.properties`:
   ```properties
   android.enableLegacyVariantApi=true
   ```
   This re-enables the old `androidApplication` + `kotlinMultiplatform` combination with AGP 9.
   
   > ⚠️ **Warning**: This is explicitly deprecated and will be **removed in AGP 10** (expected H2 2026).
   > Use only as emergency measure.

3. **Pin old versions**: Keep AGP at 8.11.2 and Gradle at 8.14.3 until the migration issues are resolved.
   These versions will remain functional until you need AGP 10 features.

---

## Verification Checklist

After applying all phases, verify the following:

- [ ] `./gradlew clean` succeeds
- [ ] `./gradlew :androidApp:assembleDebug` produces APK
- [ ] `./gradlew :composeApp:compileKotlinIosArm64` succeeds
- [ ] `./gradlew :composeApp:compileKotlinIosSimulatorArm64` succeeds
- [ ] `./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64` succeeds
- [ ] `./gradlew assemble` (full project) succeeds
- [ ] App launches on Android emulator and shows expected UI
- [ ] Xcode build for iOS simulator succeeds
- [ ] No deprecation warnings about legacy variant API
- [ ] `./gradlew :composeApp:dependencies` shows no `com.android.application` references
- [ ] `./gradlew projects` shows both `:androidApp` and `:composeApp`

---

## Quick Reference: File Change Summary

| File | Action | Phase |
|------|--------|-------|
| `androidApp/build.gradle.kts` | **Create** | 1 |
| `androidApp/src/main/AndroidManifest.xml` | **Create** | 1 |
| `androidApp/src/main/kotlin/com/jermey/seal/MainActivity.kt` | **Move** from composeApp | 1 |
| `androidApp/src/main/res/**` | **Move** from composeApp | 1 |
| `composeApp/build.gradle.kts` | **Modify** (major rewrite) | 2 |
| `composeApp/src/androidMain/kotlin/.../MainActivity.kt` | **Delete** | 7 |
| `composeApp/src/androidMain/res/` | **Delete** | 7 |
| `composeApp/src/androidMain/AndroidManifest.xml` | **Simplify or delete** | 7 |
| `gradle/libs.versions.toml` | **Modify** (AGP version + plugins) | 3 |
| `gradle/wrapper/gradle-wrapper.properties` | **Modify** (Gradle version) | 4 |
| `build.gradle.kts` (root) | **Modify** (plugin declarations) | 5 |
| `settings.gradle.kts` | **Modify** (add androidApp) | 6 |
| `gradle.properties` | **No changes** | — |
| `iosApp/**` | **No changes** | — |
