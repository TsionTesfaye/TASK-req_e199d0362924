# Community Rescue & Clinic Billing Hub

Internal-network full-stack system for managing rescue operations and clinic billing.

## Architecture

| Layer | Technology |
|---|---|
| Frontend | Vue 3 + Vite 4.5, Pinia, Vue Router, Axios |
| Backend | Spring Boot 3.2.5, Java 17, Maven |
| Database | MySQL 8.0 (prod), H2 (unit tests) |
| Migrations | Flyway V1–V6 |
| Auth | Custom session tokens + CSRF (X-Session-Token, X-CSRF-Token) |
| Encryption | AES-256-GCM for patient PII at rest |
| Containerization | Docker Compose (multi-service) |
| Testing | JUnit 5 + JaCoCo (≥90% line coverage), Vitest + @vue/test-utils, Node.js API tests |

## Tech Stack

- **Backend:** Spring Boot 3.2.5 / Java 17 / Maven — thin controllers, service-layer business logic
- **Frontend:** Vue 3 (Composition API) / Vite 4.5 / Pinia stores / Axios HTTP client
- **Database:** MySQL 8.0 — Flyway schema migrations, org-scoped JPA repositories
- **Security:** Custom `AuthFilter` — `X-Session-Token` + `X-CSRF-Token` on all mutating endpoints
- **Encryption:** `CryptoService` — AES-256-GCM, patient PII (name, phone, address) encrypted at rest
- **Testing:** JUnit 5 integration tests against H2, Vitest component tests with jsdom, Node.js E2E API tests

## Prerequisites

- Docker Engine 24+ / Docker Desktop with Compose v2
- No host-side Java or Node installation required — the build runs entirely in containers
- Optional (for development outside Docker): Java 17, Maven 3.9+, Node 18 LTS

## Seeded Credentials

A fresh database starts with **zero users**. There are no hardcoded accounts or default passwords. All credentials are created through the bootstrap flow on first launch.

| Step | How to create |
|---|---|
| Admin account | Visit `https://localhost:15443`, complete the bootstrap form |
| Additional users | Log in as admin → Admin → Users → Create User |

### Role reference

| Role | Permissions |
|---|---|
| `ADMIN` | Users, audit log, backups, restore-test logging, retention |
| `BILLING` | Invoices, payments, void, refund, daily close, exports |
| `CLINICIAN` | Open/close visits, view patients (read-only PII reveal) |
| `FRONT_DESK` | Register patients, appointments |
| `QUALITY` | QA overrides, corrective actions, sampling runs |
| `MODERATOR` | Moderate incidents/bulletins, rank/promote content |

### Test-only credentials (run_tests.sh / JUnit)

These accounts exist only inside the Docker test stack or H2 in-memory database and are destroyed after each test run:

| Username | Password | Role | Context |
|---|---|---|---|
| `admin` | `strongPass123` | `ADMIN` | API integration tests only |
| `front_desk` | `strongPass123` | `FRONT_DESK` | API integration tests only |
| `clinician` | `strongPass123` | `CLINICIAN` | API integration tests only |
| `billing` | `strongPass123` | `BILLING` | API integration tests only |
| `quality_user` | `strongPass123` | `QUALITY` | API integration tests only |
| `mod_user` | `strongPass123` | `MODERATOR` | API integration tests only |

## Quick Start

```bash
docker compose up --build
```

A dev-only `ENCRYPTION_KEY` default is provided so the stack starts without any setup. To use a real key (recommended for anything beyond local demo):

```bash
export ENCRYPTION_KEY="$(openssl rand -hex 32)"
docker compose up --build
```

Once the stack is up, verify the backend is healthy:

```bash
curl -sk https://localhost:15443/api/health
# {"data":{"status":"ok"}}
```

- **Frontend (HTTPS):** https://localhost:15443
  - Plain HTTP on `:15173` redirects to HTTPS automatically.
  - Self-signed cert (`CN=rescuehub.local`). Accept the browser warning for local development.
- **Backend API:** only reachable through the TLS proxy at `https://localhost:15443/api`. No direct host port.
- **MySQL:** not exposed to the host. Use `docker compose -p rescuehub exec mysql mysql -urescue -prescuepw rescuehub` for DBA access.

## Run All Tests

```bash
./run_tests.sh
```

Runs in Docker — no host-side Java or Node required.

| Stage | What runs | Tool |
|---|---|---|
| Backend unit tests | JUnit 5 against H2 in-memory DB, JaCoCo ≥90% line coverage | `mvn verify` inside `backend-tests` container |
| Frontend component tests | Vitest + @vue/test-utils against jsdom | `npm test` inside `frontend-tests` container |
| API integration tests | Node.js real HTTP calls against live backend + MySQL | `node api.test.js` inside `api-tests` container |

All three stages must pass for the script to exit 0.

## Project Layout

```
backend/          Spring Boot 3.2.5, Java 17, Maven
  src/main/       Application source (controllers, services, entities, repositories)
  src/test/       JUnit 5 integration tests (H2 profile)
  Dockerfile      Production image (eclipse-temurin:17)
  Dockerfile.test JUnit test runner image

frontend/         Vue 3 + Vite 4.5 (Node 18), nginx runtime
  src/features/   Feature-sliced views (patients, visits, billing, …)
  src/components/ Shared components (DataTable, ConfirmDialog, SensitiveField, …)
  src/tests/      Vitest component + client interceptor tests
  Dockerfile      Production image (nginx TLS terminator)
  Dockerfile.test Vitest test runner image

tests/            Node.js API integration tests (real HTTP calls, no mocks)
  api.test.js     Full E2E scenario: bootstrap → users → patients → visits → billing → exports

storage/          Mounted Docker volume: media, exports, backups, routesheets
docker-compose.yml
run_tests.sh
.env.example
```

## Security Model

### CSRF + TLS

- All ingress is HTTPS. The nginx container terminates TLS on `:443` and reverse-proxies `/api/` to Spring Boot on the internal Docker network.
- Authentication: `X-Session-Token` header, issued by `POST /api/auth/login` and `POST /api/bootstrap`.
- CSRF: `X-CSRF-Token` header. Every `POST/PUT/PATCH/DELETE` on `/api/*` (except bootstrap/login/health) is rejected without a valid CSRF token matching `sha256(sessionToken) == UserSession.csrfTokenHash`.

### Why a custom CSRF filter instead of Spring Security's built-in

Spring's `CsrfFilter` is designed for `HttpSession` + `JSESSIONID`. This stack uses stateless custom session tokens bound to a `UserSession` database row. Enabling both would create two competing CSRF systems. `AuthFilter` implements a single, unified CSRF gate covering all mutating paths.

## First-Run Bootstrap

On first visit, the frontend calls `GET /api/bootstrap/status`. If `initialized=false`, the user is redirected to `/bootstrap` to create the administrator account.

- `GET /api/bootstrap/status` → `{ initialized: boolean }`
- `POST /api/bootstrap` — only accepted while no users exist. Returns `{ sessionToken, csrfToken, user, userId, organizationId }`.
- Bootstrap is rate-limited: 5 attempts / 10 minutes per workstation.

## Key Business Rules (Enforced in Backend)

- **Discount cap:** $200 aggregate per invoice.
- **Billing order:** services → package rules → discounts (capped $200) → tax → total.
- **Void:** rejected after `businessDate 23:00` (cutoff is `businessDate.atTime(23,0)`) or if the invoice's business date is already daily-closed.
- **Refund:** rejected if >30 days since `invoice.generated_at`, or amount exceeds refundable balance.
- **Bulk export:** >500 rows requires `elevated=true` + `secondConfirmation=true` + `BILLING/ADMIN` role.
- **Login rate limit:** 10 attempts / 10 minutes per workstation.
- **Sampling:** `hash(visitId + seed) % 100 < percentage` — deterministic and reproducible.
- **Retention:** 7-year archive job runs daily; legal holds suspend archival.
- **Cross-tenant isolation:** every repository query is scoped to `organizationId`. Cross-org data access throws `NotFoundException`.

## Encryption Key Management

Patient PII (name, phone, address) is encrypted at rest with AES-256-GCM. There is no default key anywhere in the codebase.

| Context | Key source |
|---|---|
| Production / staging | `ENCRYPTION_KEY` environment variable (≥32 bytes); generate with `openssl rand -hex 32` |
| Local Docker (no env set) | `dev-only-insecure-key-change-before-prod!!` (docker-compose.yml default — never use in production) |
| JUnit unit tests (`@ActiveProfiles("test")`) | Fixed 32-char key in `application-test.yml` (public, intentional) |
| API integration tests (`run_tests.sh`) | `test-only-key-32-bytes-for-ci-runs-00` (set by `run_tests.sh` if `ENCRYPTION_KEY` is unset) |

Generate a production key: `openssl rand -hex 32` (64 hex chars = 32 bytes). Store in your secrets manager.

## Risk Scoring (Ephemeral by Design)

`RiskScoreService` records anomalous-login and repeated-export telemetry in process memory only. Scores reset on restart. Risk scores are **never read as an authorization input** — every enforcement point (role checks, rate limits, idempotency, export elevation) is independent. Persistent evidence is written to the immutable `AuditLog`.
