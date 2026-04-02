#!/usr/bin/env bash
# Usage:
#   ./run_local.sh              -> loads .env.shared + .env.local (default)
#   ./run_local.sh production   -> loads .env.shared + .env.production

set -euo pipefail

MODE="${1:-local}"
ENV_SHARED=".env.shared"
ENV_MODE=".env.${MODE}"

if [ ! -f "$ENV_SHARED" ]; then
  echo "Error: $ENV_SHARED not found. Create it with shared settings." >&2
  exit 1
fi

if [ ! -f "$ENV_MODE" ]; then
  echo "Error: $ENV_MODE not found. Create it for '${MODE}' mode." >&2
  exit 1
fi

echo "Loading env files: $ENV_SHARED + $ENV_MODE"

set -a
source "$ENV_SHARED"
source "$ENV_MODE"
set +a

./mvnw spring-boot:run
