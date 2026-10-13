#!/usr/bin/env sh
# Creates .env with a random database password (only if it does not exist yet).
set -eu
root=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
env_file="$root/.env"
if [ -f "$env_file" ]; then
  echo ".env já existe; nada a fazer."
  exit 0
fi
password=$(head -c 32 /dev/urandom | od -An -tx1 | tr -d ' \n')
umask 077
sed "s/^POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$password/" "$root/.env.example" > "$env_file"
echo ".env criado. Inicie com: docker compose up -d --build"
