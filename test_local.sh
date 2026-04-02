#!/usr/bin/env bash
# Usage:
#   ./test_local.sh              -> loads .env.shared + .env.local (default)
#   ./test_local.sh production   -> loads .env.shared + .env.production
#
# Runs backend test suite with layered env files.

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

./mvnw test
