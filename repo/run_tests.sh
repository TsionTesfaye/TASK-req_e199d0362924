#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"

# The stack requires ENCRYPTION_KEY (32+ bytes). For CI/local runs we provide a
# test-only value here — production deployments MUST override via the
# operator's secret manager before invoking `docker compose up`.
export ENCRYPTION_KEY="${ENCRYPTION_KEY:-test-only-key-32-bytes-for-ci-runs-00}"

COMPOSE="docker compose -p rescuehub"

echo "=== Building images ==="
$COMPOSE build backend frontend api-tests backend-tests frontend-tests e2e-tests

echo "=== Running backend unit tests (H2) ==="
$COMPOSE run --rm backend-tests

echo "=== Running frontend component tests (Vitest) ==="
$COMPOSE run --rm frontend-tests

echo "=== Starting stack for API tests (fresh volumes, mysql + backend + frontend TLS proxy) ==="
$COMPOSE down -v >/dev/null 2>&1 || true
$COMPOSE up -d mysql backend frontend

echo "=== Waiting for backend health (via TLS reverse proxy https://localhost:15443) ==="
for i in $(seq 1 60); do
  if curl -sfk https://localhost:15443/api/health 2>/dev/null | grep -q ok; then
    echo "Backend up"; break
  fi
  sleep 2
done

echo "=== Running API integration tests ==="
$COMPOSE run --rm api-tests

echo "=== Running E2E browser tests (Playwright) ==="
$COMPOSE run --rm e2e-tests

echo "=== All tests passed ==="
$COMPOSE down
