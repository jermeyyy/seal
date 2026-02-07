# Seal — Suggested Commands

## Build Commands

```bash
# Build all modules
./gradlew assemble

# Build only Android debug (demo app)
./gradlew :androidApp:assembleDebug

# Build specific library module
./gradlew :seal-core:assemble
./gradlew :seal-android:assembleDebug
./gradlew :seal-ios:compileKotlinIosArm64
./gradlew :seal-ktor:assemble

# Build iOS framework
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Testing Commands

```bash
# Run all tests
./gradlew allTests

# Run common tests only
./gradlew :seal-core:allTests

# Run Android unit tests
./gradlew :seal-core:testDebugUnitTest
./gradlew :seal-android:testDebugUnitTest

# Run iOS tests
./gradlew :seal-core:iosSimulatorArm64Test
```

## Gradle Utility Commands

```bash
# List all projects/modules
./gradlew projects

# Sync/refresh dependencies
./gradlew --refresh-dependencies

# Check dependency resolution
./gradlew dependencies

# Clean build
./gradlew clean

# Gradle wrapper version
./gradlew --version
```

## iOS (Xcode)
- Open `iosApp/` in Xcode
- Build & run from Xcode for iOS simulator/device

## Git Commands (macOS / Darwin)

```bash
git status
git add -A && git commit -m "message"
git push origin main
git log --oneline -10
git diff
```

## File System (Darwin)

```bash
ls -la
find . -name "*.kt" -not -path "./.gradle/*"
grep -r "pattern" --include="*.kt" .
```

## Publishing (when configured)

```bash
# Publish to Maven Local for testing
./gradlew publishToMavenLocal

# Publish to Maven Central (requires GPG + credentials)
./gradlew publish
```
