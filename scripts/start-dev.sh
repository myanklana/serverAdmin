#!/usr/bin/env sh
# Starts the development database, the API (:8080) and the panel (:5173) from source.
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
cd "$root"
docker compose -f docker-compose.dev.yml up -d
[ -d frontend/node_modules ] || (cd frontend && npm ci)
(cd api && ./mvnw spring-boot:run) &
api_pid=$!
trap 'kill $api_pid 2>/dev/null || true' EXIT INT TERM
cd frontend && npm run dev
