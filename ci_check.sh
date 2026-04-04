#!/usr/bin/env bash
# Usage:
#   ./ci_check.sh        -> loads .env.shared + .env.ci (default)
#   ./ci_check.sh ci     -> same as default
#
# Runs a CI-parity backend check against a hosted Supabase database:
# 1) apply pending migrations
# 2) run maven clean verify with CI-style env vars

set -eu
(set -o pipefail) 2>/dev/null && set -o pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "$0")" && pwd)"

MODE="${1:-ci}"
ENV_SHARED="${SCRIPT_DIR}/.env.shared"
ENV_MODE="${SCRIPT_DIR}/.env.${MODE}"

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
. "$ENV_SHARED"
. "$ENV_MODE"
set +a

require_env() {
  local name="$1"
  if [ -z "${!name:-}" ]; then
    echo "Error: $name is required." >&2
    exit 1
  fi
}

require_env CI_DB_URL_JDBC
require_env CI_DB_USER
require_env CI_DB_PASSWORD
require_env CI_SUPABASE_JWKS_URL

if ! command -v supabase >/dev/null 2>&1; then
  echo "Error: supabase CLI not found in PATH." >&2
  exit 1
fi

if ! command -v python3 >/dev/null 2>&1; then
  echo "Error: python3 is required to URL-encode CI_DB_PASSWORD." >&2
  exit 1
fi

JDBC_PREFIX="jdbc:postgresql://"
if [ "${CI_DB_URL_JDBC#${JDBC_PREFIX}}" = "$CI_DB_URL_JDBC" ]; then
  echo "Error: CI_DB_URL_JDBC must start with ${JDBC_PREFIX}" >&2
  exit 1
fi

ENCODED_DB_PASSWORD="$(python3 -c 'import os, urllib.parse; print(urllib.parse.quote(os.environ["CI_DB_PASSWORD"], safe=""))')"
PG_URL="postgresql://${CI_DB_USER}:${ENCODED_DB_PASSWORD}@${CI_DB_URL_JDBC#${JDBC_PREFIX}}"

echo "Applying pending Supabase migrations to hosted CI DB..."
supabase migration up --db-url "$PG_URL"

echo "Running backend CI verify (mvn clean verify)..."
SPRING_PROFILES_ACTIVE="ci" \
CI_DB_URL_JDBC="$CI_DB_URL_JDBC" \
CI_DB_USER="$CI_DB_USER" \
CI_DB_PASSWORD="$CI_DB_PASSWORD" \
CI_SUPABASE_JWKS_URL="$CI_SUPABASE_JWKS_URL" \
"${SCRIPT_DIR}/mvnw" -B clean verify
