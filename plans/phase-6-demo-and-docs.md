# Phase 6: Demo App, Documentation & Docs Website

> **Prerequisites**: All Phases 0-5 to be substantially complete
> **Summary**: Update the `composeApp` demo application with OkHttp and Ktor CT demonstration screens, add comprehensive KDoc to all public API surfaces, configure Dokka for HTML API docs generation, build a React documentation website (reusing design/components from quo-vadis), write the root README, and set up GitHub Actions for automatic deployment.

> **Read [00-architecture-overview.md](00-architecture-overview.md) first for full architectural context.**

---

## Phase Dependencies

Requires all Phases 0-5 to be substantially complete.

---

## Dependency Graph (Phase 6)

```
6.1  OkHttp demo screen             ← 3.5, 3.6
6.2  Ktor demo screen               ← 5.1, 5.2, 5.3
6.3  KDoc API docs                   ← All phases
6.4  Dokka setup & generation        ← 6.3
6.5  Docs site scaffold (React)      ← None (infrastructure)
6.6  Docs site pages & content       ← 6.5, All phases
6.7  Search index & build pipeline   ← 6.6
6.8  GitHub Actions deploy workflow  ← 6.4, 6.7
6.9  README                          ← All phases
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
- All `public` classes, interfaces, functions, properties in `seal-core`, `seal-android`, `seal-ios`, `seal-ktor`
- All `public` sealed class variants
- Builder DSL methods
- Module-level documentation (`package.md` or module docs)

**Dependencies**: All prior phases
**Acceptance Criteria**: All public APIs have KDoc; zero undocumented public symbols
**Complexity**: Medium

---

### Task 6.4: Dokka Setup & Multi-Module HTML Generation

**Description**: Configure Dokka 2.x for multi-module HTML documentation generation across all library modules (`seal-core`, `seal-android`, `seal-ios`, `seal-ktor`). Follows the same pattern as quo-vadis.

**Files to create/modify**:
- `gradle/libs.versions.toml` (add Dokka version if not present)
- `build.gradle.kts` (root — apply Dokka plugin, configure `dokkaGeneratePublicationHtml`)
- `seal-core/build.gradle.kts` (Dokka source set config)
- `seal-android/build.gradle.kts` (Dokka source set config)
- `seal-ios/build.gradle.kts` (Dokka source set config)
- `seal-ktor/build.gradle.kts` (Dokka source set config)
- `gradle.properties` (add Dokka V2 properties)

**Configuration details**:
```kotlin
// Root build.gradle.kts
plugins {
    id("org.jetbrains.dokka") version "<version>"
}

// Each module's build.gradle.kts
dokka {
    moduleName.set("Seal - <module name>")
    moduleVersion.set(project.version.toString())
    
    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
        suppressObviousFunctions.set(true)
        suppressInheritedMembers.set(false)
    }
    
    dokkaSourceSets.configureEach {
        sourceLink { /* GitHub source links */ }
        externalDocumentationLinks.create("android") { /* ... */ }
        externalDocumentationLinks.create("coroutines") { /* ... */ }
        perPackageOption {
            matchingRegex.set(".*\\.internal.*")
            suppress.set(true)
        }
    }
}
```

```properties
# gradle.properties
org.jetbrains.dokka.experimental.gradle.pluginMode=V2Enabled
org.jetbrains.dokka.experimental.gradle.pluginMode.noWarn=true
```

**Dependencies**: 6.3
**Acceptance Criteria**: `./gradlew dokkaGeneratePublicationHtml --no-daemon` generates complete multi-module HTML docs at `build/dokka/html/`
**Complexity**: Medium

---

### Task 6.5: Documentation Website Scaffold (React + Vite)

**Description**: Create a React + TypeScript + Vite documentation website at `docs/site/`, reusing the **same design, styling, UI components, and architecture** from the [quo-vadis docs site](https://github.com/jermeyyy/quo-vadis/tree/main/docs/site). The site should be a near-identical shell with Seal-specific content and branding.

**Directory structure to create**:
```
docs/site/
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── eslint.config.js
├── index.html                          ← SPA redirect script for GitHub Pages
├── public/
│   ├── 404.html                        ← SPA redirect handler
│   ├── favicon.ico
│   ├── favicon-16x16.png
│   ├── favicon-32x32.png
│   ├── apple-touch-icon.png
│   └── logo.jpg                        ← Seal logo (create or use placeholder)
├── scripts/
│   └── buildSearchIndex.js             ← Copy from quo-vadis, adjust paths
└── src/
    ├── main.tsx                         ← Entry point (copy from quo-vadis)
    ├── App.tsx                          ← Router + Layout (adapt routes for Seal)
    ├── App.css                          ← Copy from quo-vadis
    ├── index.css                        ← Copy from quo-vadis
    ├── styles/
    │   ├── global.css                   ← Copy from quo-vadis (identical)
    │   └── variables.css                ← Copy from quo-vadis (identical CSS vars)
    ├── contexts/
    │   ├── ThemeContext.tsx              ← Copy from quo-vadis (identical)
    │   └── SearchContext.tsx             ← Copy from quo-vadis (identical)
    ├── components/                      ← REUSE from quo-vadis
    │   ├── CodeBlock/                   ← Copy: CodeBlock.tsx, CodeBlock.module.css, codeblock-theme.css
    │   ├── Layout/                      ← Copy: Layout.tsx, Navbar.tsx, Sidebar.tsx, TableOfContents.tsx + .module.css files
    │   ├── MobileMenu/                  ← Copy: MobileMenu.tsx + .module.css
    │   ├── Search/                      ← Copy: SearchBar.tsx, SearchModal.tsx + .module.css files
    │   └── ThemeToggle/                 ← Copy: ThemeToggle.tsx + .module.css
    ├── data/
    │   ├── constants.ts                 ← Seal-specific (version, artifacts, URLs)
    │   ├── navigation.ts                ← Seal-specific navigation structure
    │   ├── codeExamples.ts              ← Seal-specific code examples
    │   └── searchData.json              ← Generated by buildSearchIndex.js
    └── pages/                           ← Seal-specific content (Task 6.6)
```

**Components to copy verbatim from quo-vadis** (same design & styling):
1. `components/CodeBlock/` — Syntax highlighting with highlight.js
2. `components/Layout/` — Navbar, Sidebar, TableOfContents, Layout wrapper
3. `components/MobileMenu/` — Mobile hamburger navigation
4. `components/Search/` — SearchBar + SearchModal (flexsearch)
5. `components/ThemeToggle/` — Dark/light mode toggle
6. `contexts/ThemeContext.tsx` — Theme provider
7. `contexts/SearchContext.tsx` — Search provider
8. `styles/global.css` — Base styles
9. `styles/variables.css` — Full CSS custom properties (light/dark themes, layout, typography)

**Adaptations required**:
- `vite.config.ts`: Change `base` from `/quo-vadis/` to `/seal/`
- `App.tsx`: Change `basename` from `/quo-vadis` to `/seal`, define Seal routes
- `Navbar.tsx`: Update title from "Quo Vadis" to "Seal", update GitHub link
- `data/constants.ts`: Seal library version, Maven artifact coordinates, repository URLs
- `data/navigation.ts`: Seal-specific sidebar navigation structure
- `index.html`: Update `<title>` to Seal

**Package.json** (same deps as quo-vadis):
```json
{
  "name": "seal-docs",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "npm run build:search && tsc -b && vite build",
    "build:search": "node scripts/buildSearchIndex.js",
    "lint": "eslint .",
    "preview": "vite preview"
  },
  "dependencies": {
    "react": "^19.1.1",
    "react-dom": "^19.1.1"
  },
  "devDependencies": {
    "@eslint/js": "^9.36.0",
    "@types/flexsearch": "^0.7.6",
    "@types/node": "^24.6.0",
    "@types/react": "^19.1.16",
    "@types/react-dom": "^19.1.9",
    "@vitejs/plugin-react": "^5.0.4",
    "eslint": "^9.36.0",
    "eslint-plugin-react-hooks": "^5.2.0",
    "eslint-plugin-react-refresh": "^0.4.22",
    "flexsearch": "^0.8.212",
    "globals": "^16.4.0",
    "highlight.js": "^11.11.1",
    "react-router-dom": "^7.9.4",
    "typescript": "~5.9.3",
    "typescript-eslint": "^8.45.0",
    "vite": "^7.1.7"
  }
}
```

**Dependencies**: None (infrastructure task)
**Acceptance Criteria**: `cd docs/site && npm install && npm run dev` starts a working dev server with Seal branding; sidebar, navbar, theme toggle, search UI all functional; all copied component styles match quo-vadis exactly
**Complexity**: Medium-High

---

### Task 6.6: Documentation Website Pages & Content

**Description**: Create all content pages for the Seal documentation website. Each page follows the same component patterns from quo-vadis (article wrapper, section structure, CodeBlock usage, CSS module styles).

**Navigation structure** (`data/navigation.ts`):
```typescript
export const navigationData: NavItem[] = [
  { label: 'Home', path: '/' },
  { label: 'Getting Started', path: '/getting-started' },
  {
    label: 'Guides',
    children: [
      { label: 'OkHttp Integration', path: '/guides/okhttp' },
      { label: 'Ktor Integration', path: '/guides/ktor' },
      { label: 'Custom Policies', path: '/guides/custom-policies' },
      { label: 'iOS Integration', path: '/guides/ios' },
      { label: 'Configuration', path: '/guides/configuration' },
    ]
  },
  { label: 'Demo App', path: '/demo' },
  {
    label: 'API Reference',
    href: '/seal/api/index.html',
    external: true
  },
  {
    label: 'GitHub',
    href: 'https://github.com/jermeyyy/seal',
    external: true,
    icon: 'github'
  }
]
```

**Pages to create**:

| Page | File | Content |
|------|------|---------|
| **Home** | `pages/Home/Home.tsx` + `.module.css` | Hero section (logo, badges, tagline), feature highlights (CT enforcement, multi-platform, OkHttp/Ktor), quick install snippet, "Show Me The Code" section, resources links. Follows quo-vadis Home.tsx pattern. |
| **Getting Started** | `pages/GettingStarted/GettingStarted.tsx` + `.module.css` | Installation (Gradle coordinates for each module), quick start code, minimal OkHttp + Ktor examples, platform requirements. Follows quo-vadis GettingStarted.tsx pattern. |
| **OkHttp Integration** | `pages/Guides/OkHttp/OkHttp.tsx` | Full OkHttp integration guide: interceptor setup, `CertificateTransparencyInterceptor`, network interceptor vs application interceptor, exclusion hosts, builder DSL, full example. |
| **Ktor Integration** | `pages/Guides/Ktor/Ktor.tsx` | Full Ktor plugin guide: `CertificateTransparencyPlugin` installation, configuration DSL, platform support, full example. |
| **Custom Policies** | `pages/Guides/CustomPolicies/CustomPolicies.tsx` | CT policy configuration: `CTPolicy` interface, custom `CTVerifier`, log sources, trust anchors, operator diversity. |
| **iOS Integration** | `pages/Guides/iOS/iOS.tsx` | iOS-specific guide: `NSURLSession` integration, platform expectations, Swift interop, CocoaPods/SPM if applicable. |
| **Configuration** | `pages/Guides/Configuration/Configuration.tsx` | Configuration reference: all builder DSL options, exclusion patterns, log list sources, cache configuration, failure modes. |
| **Demo App** | `pages/Demo/Demo.tsx` + `.module.css` | Demo app documentation: screenshots, how to run, what it demonstrates, code examples. Follows quo-vadis Demo.tsx pattern. |

**Shared styles**: Create `pages/Guides/Guides.module.css` — copy from quo-vadis `pages/Features/Features.module.css` (shared feature page styles: intro class, note class, callout, tables, etc.)

**data/constants.ts**:
```typescript
export const LIBRARY_VERSION = '0.1.0';  // update on release

export const MAVEN_ARTIFACTS = {
  core: 'io.github.jermeyyy:seal-core',
  android: 'io.github.jermeyyy:seal-android',
  ios: 'io.github.jermeyyy:seal-ios',
  ktor: 'io.github.jermeyyy:seal-ktor',
} as const;

export const REPOSITORY_URLS = {
  github: 'https://github.com/jermeyyy/seal',
  mavenCentral: 'https://central.sonatype.com/artifact/io.github.jermeyyy/seal-core',
  apiDocs: '/seal/api/index.html',
} as const;
```

**data/codeExamples.ts**: Centralize all Kotlin code examples used across pages (installation snippets, OkHttp setup, Ktor plugin, custom policy, iOS usage, etc.)

**Dependencies**: 6.5, All phases
**Acceptance Criteria**: All pages render correctly; code examples are syntactically valid Kotlin; navigation works between all pages; table of contents auto-generates from `h2`/`h3` headings; mobile responsive
**Complexity**: High

---

### Task 6.7: Search Index & Build Pipeline

**Description**: Set up the client-side search functionality using flexsearch (same as quo-vadis), including the build-time search index generation script.

**Files to create/modify**:
- `docs/site/scripts/buildSearchIndex.js` — Copy from quo-vadis, adjust `pagesDir` path. Extracts text content from `.tsx` page files and generates `searchData.json`.
- `docs/site/src/data/searchData.json` — Auto-generated (gitignored or committed)

**Build pipeline verification**:
```bash
cd docs/site
npm run build:search  # generates searchData.json
npm run build         # full build: search index + TypeScript + Vite
npm run preview       # preview production build locally
```

**Dependencies**: 6.6
**Acceptance Criteria**: `npm run build` completes without errors; search modal (`Ctrl+K` / `Cmd+K`) finds content from all pages; search results link to correct pages
**Complexity**: Low

---

### Task 6.8: GitHub Actions Deploy Workflow

**Description**: Create a GitHub Actions workflow to automatically build and deploy the docs site + Dokka API docs to GitHub Pages on push to `main`. Follows the same pattern as quo-vadis `deploy-pages.yml`.

**File to create**: `.github/workflows/deploy-pages.yml`

```yaml
name: Deploy GitHub Pages

on:
  push:
    branches: [ main ]
  workflow_dispatch:

permissions:
  contents: read
  pages: write
  id-token: write

concurrency:
  group: "pages"
  cancel-in-progress: true

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4
      
      - name: Setup Node.js
        uses: actions/setup-node@v4
        with:
          node-version: '20'
          cache: 'npm'
          cache-dependency-path: docs/site/package-lock.json
      
      - name: Setup Java
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
      
      - name: Setup Gradle
        uses: gradle/gradle-build-action@v3
      
      - name: Install npm dependencies
        working-directory: docs/site
        run: npm ci
      
      - name: Build React documentation site
        working-directory: docs/site
        run: npm run build
      
      - name: Generate Dokka multi-module documentation
        run: ./gradlew dokkaGeneratePublicationHtml --no-daemon
      
      - name: Prepare GitHub Pages content
        run: |
          mkdir -p _site
          cp -r docs/site/dist/* _site/
          cp -r build/dokka/html _site/api
      
      - name: Upload artifact
        uses: actions/upload-pages-artifact@v3
        with:
          path: '_site'
  
  deploy:
    needs: build
    runs-on: ubuntu-latest
    environment:
      name: github-pages
      url: ${{ steps.deployment.outputs.page_url }}
    steps:
      - name: Deploy to GitHub Pages
        id: deployment
        uses: actions/deploy-pages@v4
```

**Result**: Site deployed at `https://jermeyyy.github.io/seal/` with API docs at `https://jermeyyy.github.io/seal/api/`

**Dependencies**: 6.4, 6.7
**Acceptance Criteria**: Push to `main` triggers workflow; site is accessible at GitHub Pages URL; API docs accessible at `/seal/api/`; React Router paths work (404.html SPA redirect)
**Complexity**: Low

---

### Task 6.9: Write README with Usage Examples

**Description**: Update the root `README.md` with comprehensive library documentation. The README should link to the docs website for detailed guides.

**Files to modify**:
- `README.md`

**Sections**:
1. Overview / Badges (Maven Central, License, Kotlin, Platform badges)
2. What is Certificate Transparency?
3. Installation (Gradle dependency coordinates for all modules)
4. Quick Start (minimal OkHttp example)
5. OkHttp Integration (link to docs site for full guide)
6. Ktor Integration (link to docs site for full guide)
7. Configuration Reference (summary, link to docs site)
8. Custom Policies (summary, link to docs site)
9. iOS Specifics (summary, link to docs site)
10. API Documentation (link to `https://jermeyyy.github.io/seal/api/`)
11. Demo App
12. FAQ / Troubleshooting
13. Contributing
14. License

**Dependencies**: All prior phases
**Acceptance Criteria**: README covers all use cases; code examples compile; links to docs site and API docs work
**Complexity**: Medium

---

## Implementation Notes

### Reuse Strategy from quo-vadis

The documentation site reuses the **exact same design system, components, and architecture** from [quo-vadis](https://github.com/jermeyyy/quo-vadis/tree/main/docs/site):

| Component | Action | Notes |
|-----------|--------|-------|
| `styles/variables.css` | Copy verbatim | CSS custom properties (colors, spacing, typography, dark theme) |
| `styles/global.css` | Copy verbatim | Base element styles |
| `components/Layout/` | Copy, minor branding changes | Update Navbar title/links |
| `components/CodeBlock/` | Copy verbatim | highlight.js syntax highlighting |
| `components/ThemeToggle/` | Copy verbatim | Dark/light theme toggle |
| `components/Search/` | Copy verbatim | SearchBar + SearchModal |
| `components/MobileMenu/` | Copy verbatim | Mobile navigation |
| `contexts/` | Copy verbatim | ThemeContext + SearchContext |
| `scripts/buildSearchIndex.js` | Copy, adjust paths | Search index generator |
| `index.html` | Copy, update title | SPA redirect for GitHub Pages |
| `public/404.html` | Copy verbatim | SPA redirect handler |
| `vite.config.ts` | Copy, change base URL | `/seal/` instead of `/quo-vadis/` |
| `tsconfig*.json` | Copy verbatim | TypeScript configuration |
| `eslint.config.js` | Copy verbatim | ESLint configuration |

**Only Seal-specific content is new**: `data/constants.ts`, `data/navigation.ts`, `data/codeExamples.ts`, and all page components under `pages/`.

### Domain-Specific Components NOT Copied
These quo-vadis components are library-specific and **not** copied:
- `NavNodeTypesTable/` — navigation node types (quo-vadis specific)
- `PlatformSupportGrid/` — platform support grid (quo-vadis specific)
- `ScopePropertiesTable/` — scope properties (quo-vadis specific)
- `TransitionTypesDisplay/` — transition animations (quo-vadis specific)

If Seal needs similar domain-specific components (e.g., CT log status table, policy comparison table), create new ones following the same CSS module pattern.

### Local Development
```bash
# Start docs site dev server
cd docs/site
npm install
npm run dev
# → http://localhost:5173/

# Generate Dokka docs locally
./gradlew dokkaGeneratePublicationHtml --no-daemon
# → build/dokka/html/index.html

# Full preview (production build)
cd docs/site
npm run build
npm run preview
# → http://localhost:4173/seal/
```
