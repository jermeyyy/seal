# Seal — Task Completion Checklist

## After Completing a Task

### 1. Verify Build
```bash
./gradlew assemble
```
Ensure all modules compile without errors.

### 2. Run Tests
```bash
./gradlew allTests
```
All existing tests must pass. If you added new functionality, ensure corresponding tests exist.

### 3. Check explicitApi Compliance
Library modules (`seal-core`, `seal-android`, `seal-ios`, `seal-ktor`) use `explicitApi()`. All public symbols must have explicit visibility modifiers (`public`, `internal`, `private`).

### 4. Verify API Consistency
- New public API should follow the DSL builder pattern where applicable
- Result types should use sealed class hierarchies
- Fail-open should be the default behavior

### 5. Check No Breaking Changes (unless intentional)
- If editing public symbols, verify references with `find_referencing_symbols`
- Ensure backward compatibility or document breaking changes

### 6. Commit
```bash
git add -A && git commit -m "descriptive message"
```

## Quality Gates
- No compiler warnings in library modules
- `explicitApi()` violations are compile errors — they block the build
- Lenient JSON parsing — never use `require(...)` on parsed data that may change
