# Phase 6: Demo App & Documentation

> **Prerequisites**: All Phases 0-5 to be substantially complete
> **Summary**: Update the `composeApp` demo application with OkHttp and Ktor CT demonstration screens, add comprehensive KDoc to all public API surfaces, and write the root README with usage examples, configuration reference, and platform-specific notes.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires all Phases 0-5 to be substantially complete.

---

## Dependency Graph (Phase 6)

```
6.1  OkHttp demo screen           ← 3.5, 3.6
6.2  Ktor demo screen             ← 5.1, 5.2, 5.3
6.3  KDoc API docs                ← All phases
6.4  README                       ← All phases
```

---

## Tasks

---

### Task 6.1: Update composeApp with OkHttp CT Demo

**Description**: Add a screen to the demo app demonstrating OkHttp with CT enforcement. Show a list of URLs being checked and their CT verification results.

**Files to modify**:
- `composeApp/build.gradle.kts` (add OkHttp + Ktor deps)
- `composeApp/src/androidMain/kotlin/com/jermey/seal/...` (OkHttp demo)
- `composeApp/src/commonMain/kotlin/com/jermey/seal/App.kt` (shared UI)

**Dependencies**: 3.5, 3.6
**Acceptance Criteria**: Demo app builds and runs; shows CT verification results for sample HTTPS connections
**Complexity**: Medium

---

### Task 6.2: Add Ktor CT Demo Screen

**Description**: Add a screen demonstrating Ktor client with CT plugin on both platforms.

**Files to modify/create**:
- `composeApp/src/commonMain/kotlin/com/jermey/seal/demo/KtorDemoScreen.kt`

**Dependencies**: 5.1, 5.2, 5.3
**Acceptance Criteria**: Ktor plugin demo works on both Android and iOS
**Complexity**: Medium

---

### Task 6.3: Write API Documentation (KDoc)

**Description**: Add comprehensive KDoc to all `public` API surfaces across all library modules.

**Scope**:
- All `public` classes, interfaces, functions, properties
- All `public` sealed class variants
- Builder DSL methods
- Module-level documentation (`package.md` or module docs)

**Dependencies**: All prior phases
**Acceptance Criteria**: `./gradlew dokkaHtml` generates full API docs; zero undocumented public APIs
**Complexity**: Medium

---

### Task 6.4: Write README with Usage Examples

**Description**: Update the root `README.md` with comprehensive library documentation.

**Files to modify**:
- `README.md`

**Sections**:
1. Overview / Badges
2. Installation (Gradle dependency coordinates)
3. Quick Start (minimal code)
4. OkHttp Integration (full example)
5. Ktor Integration (full example)
6. Configuration Reference
7. Custom Policies
8. iOS Specifics
9. FAQ / Troubleshooting
10. Contributing
11. License

**Dependencies**: All prior phases
**Acceptance Criteria**: README covers all use cases; code examples compile
**Complexity**: Medium
