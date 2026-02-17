# Omnibus Frontend

> React 19 single-page application for the Omnibus financial transfer platform.

---

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Framework | React | 19.2 |
| Language | TypeScript | 5.9 |
| Bundler | Vite | 6.4 |
| Styling | Tailwind CSS | 3.4 |
| Routing | React Router | 7.13 |
| Server State | TanStack React Query | 5.90 |
| HTTP Client | Axios | 1.13 |
| Forms | React Hook Form | 7.71 |
| Validation | Zod | 4.3 |
| UI Primitives | Radix UI | latest |
| Icons | Lucide React | 0.570 |
| Notifications | Sonner | 2.0 |

## Project Structure

```
src/
├── main.tsx                  # Entry point — QueryClientProvider, BrowserRouter
├── App.tsx                   # Route definitions, AuthProvider, ErrorBoundary
├── index.css                 # Tailwind directives + CSS custom properties (theming)
├── components/
│   ├── DashboardLayout.tsx   # Sidebar + content shell for authenticated pages
│   ├── ErrorBoundary.tsx     # Global error boundary with fallback UI
│   ├── ProtectedRoute.tsx    # Auth guard — redirects to /login if unauthenticated
│   ├── Sidebar.tsx           # Navigation sidebar with account info
│   └── Skeleton.tsx          # Loading placeholder component
├── contexts/
│   └── AuthContext.tsx       # JWT auth state, login/register/logout, Axios interceptor
├── hooks/
│   └── useTheme.ts           # Dark/light/system theme persistence (localStorage)
├── lib/
│   ├── api.ts                # Axios instance + typed API functions (auth, accounts, transfers)
│   └── utils.ts              # cn() helper (clsx + tailwind-merge)
├── pages/
│   ├── LoginPage.tsx         # Login form with Zod validation
│   ├── RegisterPage.tsx      # Registration form with Zod validation
│   ├── DashboardPage.tsx     # Account overview, balance, recent transactions
│   ├── SendMoneyPage.tsx     # Transfer form with recipient lookup
│   ├── ActivityPage.tsx      # Full transaction history with pagination
│   └── NotFoundPage.tsx      # 404 fallback
└── types/
    └── index.ts              # Shared TypeScript interfaces (User, Account, Transaction, etc.)
```

## Getting Started

### Prerequisites

- **Node.js** 18+ and npm
- **Omnibus backend** running on `http://localhost:8080`

### Development

```bash
# Install dependencies
npm install

# Start dev server (proxies /api → localhost:8080)
npm run dev
```

The dev server starts at `http://localhost:5173` with hot module replacement. All `/api` requests are proxied to the backend via Vite's built-in proxy.

### Available Scripts

| Script | Command | Description |
|--------|---------|-------------|
| `dev` | `vite` | Start development server with HMR on port 5173 |
| `build` | `tsc -b && vite build` | Type-check then produce optimized production bundle |
| `lint` | `eslint .` | Run ESLint with TypeScript-aware rules |
| `preview` | `vite preview` | Serve the production build locally for verification |

### Production Build

```bash
npm run build    # Output → dist/
npm run preview  # Preview production build at http://localhost:4173
```

The build is optimized with manual chunk splitting:
- **vendor** — `react`, `react-dom`, `react-router-dom`
- **query** — `@tanstack/react-query`, `axios`
- **ui** — `lucide-react`, `sonner`, `clsx`, `tailwind-merge`, `class-variance-authority`
- **forms** — `react-hook-form`, `@hookform/resolvers`, `zod`

## Architecture Decisions

### Authentication
JWT tokens are stored in `localStorage` and attached to every API request via an Axios request interceptor in `AuthContext`. On 401 responses, the interceptor automatically clears the token and redirects to `/login`.

### Server State Management
TanStack React Query handles all server state with automatic caching, background refetching, and stale-while-revalidate. Query keys are scoped per resource (`accounts`, `transactions`, `user`).

### Form Validation
All forms use React Hook Form with Zod schema resolvers. Validation rules mirror the backend's Jakarta Bean Validation constraints to provide immediate client-side feedback.

### Theming
CSS custom properties in `index.css` define a complete design token system for light and dark modes. The `useTheme` hook persists the user's preference to `localStorage` and supports a `system` option that follows `prefers-color-scheme`.

### Path Aliases
The `@/` alias resolves to `src/`, configured in both `vite.config.ts` (for bundling) and `tsconfig.app.json` (for TypeScript).

## API Integration

The frontend communicates with the Omnibus REST API through typed functions in `lib/api.ts`:

| Function | Method | Endpoint | Description |
|----------|--------|----------|-------------|
| `loginUser` | POST | `/api/auth/login` | Authenticate and receive JWT |
| `registerUser` | POST | `/api/auth/register` | Register new user + account |
| `getCurrentUser` | GET | `/api/auth/me` | Fetch authenticated user profile |
| `getAccount` | GET | `/api/accounts/{id}` | Get account details with balance |
| `lookupAccount` | GET | `/api/accounts/lookup` | Find account by username |
| `getTransactions` | GET | `/api/accounts/{id}/transactions` | List account transactions |
| `createTransfer` | POST | `/api/transfers` | Execute a money transfer |

## Configuration

### Vite Proxy (`vite.config.ts`)

```typescript
proxy: {
  '/api': {
    target: 'http://localhost:8080',
    changeOrigin: true,
    secure: false,
  },
}
```

All `/api` paths are forwarded to the Spring Boot backend during development. In production, configure a reverse proxy (e.g., Nginx) to route API traffic.

---

*Part of the [Omnibus](../README.md) financial transfer platform.*
