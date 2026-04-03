# Local Development Guide

## 1) Prerequisites

- Java 21
- Docker Desktop or OrbStack (must be running)
- Supabase CLI

## 2) Install Supabase CLI

Install Supabase CLI using one of these methods:

See platform-specific instructions:

- [Supabase CLI Install Guide (Windows + macOS)](./supabase-cli-install.md)

Project-pinned npm method (recommended):

```bash
npm install supabase --save-dev
npx supabase --version
```

Global install methods (brew/scoop) are also documented in the guide above.

```bash
supabase --version
```

## 3) Create local env files

```bash
cp .env.shared.example .env.shared
cp .env.example .env.local
```

## 4) Start Supabase and get local keys

Check status first:

```bash
supabase status
```

If not running, start it:

```bash
supabase start
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

## 5) Initialize or update local DB (choose one)

### Option A (recommended for first-time setup): clean reset

```bash
supabase db reset
```

This rebuilds local DB from all migrations and runs `supabase/seed.sql` automatically.

### Option B (keep existing data): no reset

```bash
supabase migration up
psql postgresql://postgres:postgres@127.0.0.1:54322/postgres -f supabase/seed.sql
```

Use this when you already have local data you want to keep.

## 6) Run backend

Default mode (`.env.shared + .env.local`):

```bash
./run_local.sh
```

Windows:

```bat
run_local.cmd
```

Production-like mode (`.env.shared + .env.production`):

```bash
./run_local.sh production
```

Windows:

```bat
run_local.cmd production
```

## 7) Run tests

Default mode:

```bash
./test_local.sh
```

Windows:

```bat
test_local.cmd
```

Production-like mode:

```bash
./test_local.sh production
```

Windows:

```bat
test_local.cmd production
```

## Local endpoints

- Supabase Studio: `http://127.0.0.1:54323`
- Health: `http://localhost:8080/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
