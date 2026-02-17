# Security Audit — Omnibus

> **Standard:** OWASP Top 10 (2021)  
> **Scope:** Full-stack application (Java backend + React frontend)

---

## OWASP Top 10 Compliance Matrix

| # | Category | Status | Notes |
|---|----------|--------|-------|
| A01 | Broken Access Control | **PASS** | Account ownership enforced on all endpoints; source account authorization on transfers |
| A02 | Cryptographic Failures | **PARTIAL** | HMAC-SHA256 JWT, BCrypt passwords; hardcoded dev secrets addressed via `application-prod.yml` |
| A03 | Injection | **PASS** | All queries parameterized (JPA/JPQL/native with `?`), Bean Validation on all inputs |
| A04 | Insecure Design | **PARTIAL** | Pessimistic locking, idempotency, deterministic ordering present; no rate limiting |
| A05 | Security Misconfiguration | **PASS** | Production profile disables debug logging, Swagger, and error disclosure |
| A06 | Vulnerable Components | **PASS** | All dependencies at latest stable versions (Spring Boot 3.5.10, Jan 2026) |
| A07 | Auth Failures | **PARTIAL** | Opaque login errors, frontend auto-logout on 401; no brute-force protection or account lockout |
| A08 | Software Integrity | **PASS** | JWT signed with HMAC-SHA256, no custom deserialization, multi-stage Docker build |
| A09 | Logging & Monitoring | **PARTIAL** | Audit trail for transfers; no auth event logging |
| A10 | SSRF | **N/A** | No outbound HTTP calls |

---

## Implemented Security Controls

### Authentication & Authorization
- **JWT bearer tokens** (HMAC-SHA256) with 15-minute expiry
- **BCrypt** password hashing (strength 10)
- **Account ownership verification** on `GET /accounts/{id}` and `POST /transfers` (source account)
- Stateless sessions — no server-side session state
- CSRF disabled (appropriate for stateless API)

### Input Validation
- `@Validated` on controllers with Bean Validation constraints
- `@DecimalMax`, `@Pattern`, `@Size`, `@Positive`, `@NotNull` on all DTO fields
- `@Size(min=1, max=255)` on Idempotency-Key header
- `@Pattern(regexp="^[A-Z]{3}$")` on currency codes
- RFC 7807 ProblemDetail error responses with `type` URIs (no stack traces)

### Data Integrity
- **Double-entry bookkeeping** — atomic DEBIT + CREDIT entries
- **Pessimistic locking** — `SELECT ... FOR UPDATE` prevents lost updates
- **Deterministic lock ordering** — ascending UUID prevents deadlocks
- **Idempotency keys** — exactly-once processing with 24h TTL
- **REQUIRES_NEW audit** — audit entries survive transaction rollbacks

### Infrastructure
- Non-root Docker container (`appuser:1001`)
- ZGC for sub-ms GC pauses
- Virtual threads for high concurrency
- `hibernate.ddl-auto=validate` — schema managed exclusively by Flyway

### Frontend Security
- **JWT stored in localStorage** — tokens attached via Axios request interceptor; cleared on logout or 401
- **Automatic session expiry** — 401 responses trigger token removal and redirect to `/login`
- **Protected routes** — `ProtectedRoute` component prevents unauthenticated access to dashboard pages
- **No sensitive data in client state** — account lookup endpoint returns only public info (username, accountNumber, accountId); balances are never exposed for other users
- **Zod input validation** — all forms validate client-side before submission, matching backend Jakarta constraints
- **Error boundary** — global `ErrorBoundary` prevents unhandled exceptions from exposing stack traces
- **No inline scripts** — React/Vite build pipeline handles all JavaScript bundling (CSP-friendly)

### Production Hardening (`application-prod.yml`)
- Database credentials via environment variables
- JWT secret via environment variable
- Swagger UI disabled by default
- Error messages suppressed (`include-message: never`)
- Logging set to INFO/WARN (no SQL or parameter logging)

---

## Known Limitations & Recommendations

### Not Implemented (Acceptable for Demo)

| Area | Recommendation | Priority |
|------|---------------|----------|
| Rate Limiting | Add Bucket4j or API gateway throttling per-user/per-endpoint | High |
| Brute-Force Protection | Track failed login attempts, lock after 5 failures for 15 min | High |
| Token Storage | Migrate JWT from `localStorage` to `httpOnly` secure cookies to mitigate XSS token theft | High |
| Auth Event Logging | Log LOGIN_SUCCESS, LOGIN_FAILED, REGISTRATION events to audit trail | Medium |
| Token Revocation | Add Redis-backed JWT blacklist for logout/compromise scenarios | Medium |
| CORS | Explicitly configure `CorsConfigurationSource` with allowed origins for production | Medium |
| CSP Header | Add `Content-Security-Policy` header via Spring Security (`script-src 'self'`, `style-src 'self'`) | Medium |
| Password Complexity | Enforce uppercase, number, special character requirements | Low |
| BCrypt Strength | Increase from 10 to 12 rounds for financial-grade security | Low |
| User Enumeration | Registration returns "Username already taken" — consider generic response | Low |
| Transfer Velocity | Add per-account daily/hourly transfer limits | Low |
| MFA | Add TOTP or WebAuthn second factor | Low |
| Refresh Tokens | Implement refresh token rotation to reduce access token lifetime to 5 min | Low |

### Dependency Scanning
- Consider adding `org.owasp:dependency-check-maven` plugin for automated CVE detection in CI
- Run `npm audit` on the frontend as part of CI pipeline

---

## Security Contact

For security concerns, contact the engineering team via the repository's security advisory feature.
