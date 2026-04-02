# Local Development Guide

## 1) Prerequisites

- Java 21
- Docker Desktop or OrbStack (must be running)
- Node.js 20+ (used only to run Supabase CLI with `npx`)

## 2) Install Supabase CLI (project-local)

Install once in this repo:

```bash
npm install -D supabase
```

Then you can run:

```bash
npx supabase --version
```

## 3) Create local env files

```bash
cp .env.shared.example .env.shared
cp .env.example .env.local
```

## 4) Start Supabase and get local keys

Check status first:

```bash
npx supabase status
```

If not running, start it:

```bash
npx supabase start
```

Run status again and copy values from the output:

- `Project URL` -> `SUPABASE_URL`
- `Publishable` -> `SUPABASE_ANON_KEY`
- `Secret` -> `SUPABASE_SERVICE_ROLE_KEY`

Keep these local DB values unless you changed ports:

- `DB_URL=jdbc:postgresql://127.0.0.1:54322/postgres`
- `DB_USER=postgres`
- `DB_PASSWORD=postgres`
- `SUPABASE_JWT_JWKS_URL=http://127.0.0.1:54321/auth/v1/.well-known/jwks.json`

Optional JWT fallback (only if you intentionally use HMAC fallback):

- `SUPABASE_JWT_SECRET=super-secret-jwt-token-with-at-least-32-characters-long`

## 5) Initialize or update local DB (choose one)

### Option A (recommended for first-time setup): clean reset

```bash
npx supabase db reset
```

This rebuilds local DB from all migrations and runs `supabase/seed.sql` automatically.

### Option B (keep existing data): no reset

```bash
npx supabase migration up
psql postgresql://postgres:postgres@127.0.0.1:54322/postgres -f supabase/seed.sql
```

Use this when you already have local data you want to keep.

## 6) Run backend

Default mode (`.env.shared + .env.local`):

```bash
./run_local.sh
```

Production-like mode (`.env.shared + .env.production`):

```bash
./run_local.sh production
```

## 7) Run tests

Default mode:

```bash
./test_local.sh
```

Production-like mode:

```bash
./test_local.sh production
```

## Local endpoints

- Health: `http://localhost:8080/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
