# Seal — Documentation Website

## Location
`docs/site/` — standalone React/TypeScript single-page application built with Vite.

## Tech Stack
- React + TypeScript
- Vite bundler
- ESLint for linting

## Pages
| Route | Directory | Content |
|-------|-----------|---------|
| Home | `src/pages/Home/` | Landing page |
| Getting Started | `src/pages/GettingStarted/` | Installation & quick start guide |
| Why CT? | `src/pages/WhyCT/` | Certificate Transparency explainer |
| OkHttp Guide | `src/pages/Guides/OkHttp/` | OkHttp interceptor integration guide |
| Ktor Guide | `src/pages/Guides/Ktor/` | Ktor plugin integration guide |
| iOS Guide | `src/pages/Guides/iOS/` | iOS integration guide |
| Configuration | `src/pages/Guides/Configuration/` | Full configuration reference |
| Custom Policies | `src/pages/Guides/CustomPolicies/` | Custom CT policy guide |
| Demo | `src/pages/Demo/` | Demo page |

## Supporting Code
- `src/components/` — Reusable React components
- `src/data/` — Data utilities
- `src/contexts/` — React contexts
- `src/styles/` — Style definitions

## Commands
```bash
cd docs/site
npm install      # Install dependencies
npm run dev      # Start dev server
npm run build    # Production build
```

## Note
This is separate from the Dokka-generated API documentation. Dokka generates KDoc API reference via:
```bash
./gradlew dokkaGeneratePublicationHtml
# Output: build/dokka/html/
```
