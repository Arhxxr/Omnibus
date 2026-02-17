# ADR-005: React Frontend Architecture

**Status:** Accepted  
**Date:** 2026-02-17

## Context

Omnibus originally served a single-page application as an inline HTML file (`src/main/resources/static/index.html`) bundled with the Spring Boot backend. This approach had significant limitations:

- No component model — all UI logic in a single file with vanilla JavaScript
- No type safety — no TypeScript, no compile-time error detection
- No tooling — no HMR, linting, or bundle optimization
- Limited scalability — adding pages (activity history, settings) was impractical
- No modern state management — manual DOM manipulation for async operations

The fintech domain demands a polished, responsive UI with proper form validation, error handling, and authentication flows.

## Decision

We adopt a **standalone React SPA** in a `frontend/` directory, decoupled from the Spring Boot backend. React 19 with TypeScript gives us the type safety and component model needed for a multi-page financial app. The key architectural choices:

- **TanStack Query** for server state — handles caching, background refetch, and loading/error states out of the box. This was the biggest win over manual fetch logic.
- **Zod + React Hook Form** for validation — schemas mirror backend Jakarta constraints, so client and server validation stay in sync.
- **Axios** over `fetch` — interceptors make JWT attachment and 401 handling trivial.
- **Vite 6** for builds — sub-second HMR and chunk splitting for production.
- **Tailwind CSS** with CSS custom properties for dark/light theming without runtime overhead.
- **Radix UI** for accessible primitives (dialog, dropdown, avatar) that don't impose styling opinions.

### Alternatives Considered

| Alternative | Reason Rejected |
|------------|-----------------|
| HTMX + Thymeleaf | Server-rendered approach limits interactivity; poor fit for real-time balance updates and complex form flows |
| Next.js | SSR/SSG unnecessary for an authenticated SPA behind login; adds deployment complexity |
| Angular | Heavier framework with opinionated structure; smaller hiring pool for fintech startups |
| Svelte/SvelteKit | Smaller ecosystem; fewer UI component libraries for financial applications |
| Keep inline HTML | Not scalable beyond a single page; no type safety, testing, or tooling |

### Dev/Prod Strategy

- **Development:** Vite dev server on `:5173` proxies `/api` → `localhost:8080` (Spring Boot)
- **Production:** `npm run build` produces a static `dist/` folder; served via reverse proxy (Nginx) or embedded in the Spring Boot jar

## Consequences

**Positive:**
- Full TypeScript coverage across 20+ source files with shared type definitions
- Zod schemas validate client-side, matching backend Jakarta constraints exactly
- TanStack Query eliminates manual loading/error state management
- Vite HMR enables sub-second feedback during development
- Manual chunk splitting (`vendor`, `query`, `ui`, `forms`) optimizes initial load time
- CSS custom property theming supports light/dark modes with system preference detection
- Error boundary prevents unhandled exceptions from crashing the entire app

**Negative:**
- Separate build step required (adds `npm install && npm run build` to CI pipeline)
- JWT in `localStorage` is vulnerable to XSS (mitigated by React's default output escaping; `httpOnly` cookies recommended for production hardening)
- Frontend and backend can drift if API contracts change without updating TypeScript types
- Additional ~250KB vendor bundle (gzipped) compared to the inline HTML approach

**Mitigations:**
- Vite proxy configuration eliminates CORS issues during development
- Shared type definitions in `types/index.ts` serve as a contract reference
- Production build with chunk splitting keeps initial load under 200KB gzipped
- `ErrorBoundary` component provides graceful degradation on runtime errors
