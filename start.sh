#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

if [[ ! -f .env ]]; then
  echo ".env not found. Copy .env.example to .env and fill in the values." >&2
  exit 1
fi

./gen-keys.sh

pkill -f 'com.application.devhub.DevHubApplication' || true

docker compose down --remove-orphans
docker compose up -d --wait

exec ./mvnw spring-boot:run
