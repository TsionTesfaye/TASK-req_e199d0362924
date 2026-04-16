#!/usr/bin/env sh
set -e
echo "Waiting for backend..."
for i in $(seq 1 60); do
  if curl -sf http://backend:8080/api/health >/dev/null 2>&1; then
    echo "Backend up"
    break
  fi
  sleep 2
done
node api.test.js
