# Rescue Hub

**Project Type: fullstack**

Rescue Hub is a Dockerized fullstack application for rescue operations, patient workflows, incident moderation, and clinic billing.

## Tech Stack

- Frontend: Vue 3, Vite, Pinia, Vue Router
- Backend: Spring Boot 3, Java 17
- Database: MySQL 8 + Flyway migrations
- Security: `X-Session-Token` + `X-CSRF-Token`
- Testing: JUnit, Vitest, API integration tests, Playwright E2E

## Startup (Hard Gate)

Start with Docker only:

```bash
docker-compose up --build
```

(Compose v2 equivalent)

```bash
docker compose up --build
```

## Access Method

- Web UI: `https://localhost:15443`
- API base: `https://localhost:15443/api`
- Health endpoint:

```bash
curl -sk https://localhost:15443/api/health
```

Expected: response body contains `"status":"ok"`.

## Verification Method

### API Verification (curl/Postman)

1. Verify health:

```bash
curl -sk https://localhost:15443/api/health
```

2. Verify bootstrap state:

```bash
curl -sk https://localhost:15443/api/bootstrap/status
```

3. Verify auth flow:

```bash
curl -sk -X POST https://localhost:15443/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"strongPass123"}'
```

Then call `GET /api/auth/me` with `X-Session-Token`, and `POST /api/auth/logout` with both `X-Session-Token` and `X-CSRF-Token`.

### Web Verification (UI Flow)

1. Open `https://localhost:15443`.
2. If prompted, complete bootstrap.
3. Log in as `admin`.
4. Confirm dashboard loads.
5. Create a patient in **Patients**.
6. Submit an incident in **Incidents**.
7. Verify billing list loads in **Billing**.

## Authentication and Demo Credentials

Authentication is required.

A fresh DB starts with no users. Bootstrap creates the first admin, then create users for each role in **Admin -> Users**.

Use this credential plan for demo/test flows:

| Role | Username | Password |
|---|---|---|
| ADMIN | `admin` | `strongPass123` |
| FRONT_DESK | `front_desk` | `strongPass123` |
| CLINICIAN | `clinician` | `strongPass123` |
| BILLING | `billing` | `strongPass123` |
| QUALITY | `quality_user` | `strongPass123` |
| MODERATOR | `mod_user` | `strongPass123` |

## Run Tests (Docker-only)

```bash
./run_tests.sh
```

`run_tests.sh` executes all suites in Docker containers:
- backend tests
- frontend tests
- API integration tests
- browser E2E tests

## Environment Rules (Strict)

- Use Dockerized execution only.
- Do not use host runtime installs to run this project (`npm install`, `pip install`, `apt-get`, or manual DB setup).
- MySQL lifecycle is managed by Docker Compose.

## Architecture Notes

- Backend API routes are under `/api/*`.
- Frontend feature modules are under `frontend/src/features`.
- Domain rules are enforced in backend services.
- Persistent artifacts are stored under `storage/`.

## Security Notes

- Use HTTPS endpoint `https://localhost:15443`.
- Mutating API requests require both `X-Session-Token` and `X-CSRF-Token`.
- Set `ENCRYPTION_KEY` securely outside local demo contexts.

## Repository Layout

- `backend/` Spring Boot API
- `frontend/` Vue app + nginx TLS proxy
- `tests/` API integration tests
- `e2e/` Playwright tests
- `docker-compose.yml` orchestration
- `run_tests.sh` unified Docker test runner
