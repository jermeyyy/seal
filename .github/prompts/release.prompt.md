---
name: release
description: Execute the full release process - bump versions, update changelog, create release tag, and push to trigger Maven Central publishing.
---

# Release Process

You are executing a release for the Seal library. The user will provide a version number as an argument (e.g., `/release 0.1.0`). The version is: `$input`

## Pre-flight Checks

Before starting, verify:
1. You are on the `main` branch
2. Working tree is clean (`git status`)
3. The version argument `$input` is valid semver (e.g., `0.1.0`, `1.0.0-beta.1`)

If any check fails, stop and inform the user.

## Step 1: Bump Versions in Gradle Module Build Files

Update `version = "..."` AND `moduleVersion.set("...")` in these 2 files:

- `seal-core/build.gradle.kts`
- `seal-ktor/build.gradle.kts`

Each file has two version strings to update:
```
version = "<old>" → version = "$input"
moduleVersion.set("<old>") → moduleVersion.set("$input")
```

## Step 2: Bump Version in Documentation Site

Update the version constant in `docs/site/src/data/constants.ts`:
```
export const LIBRARY_VERSION = '<old>' → export const LIBRARY_VERSION = '$input'
```

## Step 3: Verify README.md

Confirm that `README.md` uses `<version>` placeholders in code examples (not hardcoded versions).
The Maven Central badge auto-resolves the latest version, so no changes are needed.
If any hardcoded version is found, update it to `$input`.

## Step 4: Update CHANGELOG.md

If `CHANGELOG.md` does not exist at the project root, create it with this structure:

```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [$input] - YYYY-MM-DD

### Added
- Initial release

### Changed

### Fixed
```

If `CHANGELOG.md` already exists:
1. Read the current `[Unreleased]` section
2. Create a new version section `## [$input] - YYYY-MM-DD` (use today's date) below `[Unreleased]`
3. Move the content from `[Unreleased]` into the new version section
4. Leave `## [Unreleased]` empty (with blank subsections) above the new version

Ask the user if they want to add/edit any changelog entries before proceeding.

## Step 5: Build Verification

Run a quick compilation check to make sure nothing is broken:
```bash
./gradlew assemble --no-configuration-cache 2>&1 | tail -5
```

If the build fails, stop and inform the user.

## Step 6: Create Release Commit and Tag

Stage all changed files and create a commit and tag:
```bash
git add -A
git commit -m "Release v$input"
git tag "v$input"
```

## Step 7: Push Commit and Tag

```bash
git push origin main
git push origin "v$input"
```

This will NOT automatically trigger the Maven Central publish workflow — that is triggered by creating a **GitHub Release** (not just a tag).

## Step 8: Show GitHub Release Link

Print the following message to the user:

---

**Release v$input tagged and pushed!**

To trigger Maven Central publishing, create a GitHub Release:

👉 https://github.com/jermeyyy/seal/releases/new?tag=v$input&title=v$input

1. Click the link above
2. Set the title to `v$input`
3. Paste the changelog entries for this version into the description
4. Click **Publish release**

This will trigger the `publish-release.yml` GitHub Action which publishes all artifacts to Maven Central.

You can monitor the workflow at: https://github.com/jermeyyy/seal/actions/workflows/publish-release.yml

---

## Summary of All Files Modified

| File | Change |
|------|--------|
| `seal-core/build.gradle.kts` | `version` and `moduleVersion` → `$input` |
| `seal-ktor/build.gradle.kts` | `version` and `moduleVersion` → `$input` |
| `docs/site/src/data/constants.ts` | `LIBRARY_VERSION` → `$input` |
| `README.md` | Verified — no hardcoded versions to update |
| `CHANGELOG.md` | New version section for `$input` |