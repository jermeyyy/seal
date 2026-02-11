# Seal — Suggested Commands

## Build Commands

```bash
# Build all modules
./gradlew assemble

# Build only Android debug (demo app)
./gradlew :androidApp:assembleDebug
# or: ./gradlew :composeApp:assembleDebug

# Build specific library module
./gradlew :seal-core:assemble
./gradlew :seal-core:jvmJar
./gradlew :seal-ktor:assemble

# Build iOS framework (for simulator)
./gradlew :composeApp:linkDebugFrameworkIosSimulatorArm64
```

## Testing Commands

```bash
# Run all tests across all modules
./gradlew allTests

# Run seal-core common tests
./gradlew :seal-core:allTests

# Run seal-core Android unit tests
./gradlew :seal-core:testDebugUnitTest

# Run seal-core iOS simulator tests
./gradlew :seal-core:iosSimulatorArm64Test

# Run seal-core JVM tests
./gradlew :seal-core:jvmTest

# Run composeApp common tests
./gradlew :composeApp:allTests
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

## Documentation (Dokka)

```bash
# Generate aggregated API docs (HTML)
./gradlew dokkaGeneratePublicationHtml

# Output at: build/dokka/html/
```

## Documentation Site (React/Vite)

```bash
cd docs/site
npm install
npm run dev        # Dev server
npm run build      # Production build
```

## Publishing

```bash
# Publish to Maven Local for testing
./gradlew publishToMavenLocal

# Publish to Maven Central (requires GPG + credentials)
./gradlew publish
```

## iOS (Xcode)
- Open `iosApp/iosApp.xcodeproj` in Xcode
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
