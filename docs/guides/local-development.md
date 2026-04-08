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

## 7) Set up Ollama for local moderation

The lesson moderation flow expects an Ollama server and the `phi3` model.

### 7.1 Install and start Ollama

Install Ollama from the official site, then start the Ollama app/service on your machine.

Check that Ollama is running locally:

```bash
ollama list
```

If the command works, Ollama is available on the default local port `11434`.

### 7.2 Pull the required model

Pull `phi3` locally:

```bash
ollama pull phi3
```

Verify it is installed:

```bash
ollama list
```

Make sure `phi3` appears in the output before running the backend.

### 7.3 Update backend Ollama URL to localhost

Open:

- `src/main/java/com/example/demo/lesson/moderation/OllamaModerationService.java`

Find the Ollama API URL inside `restTemplate.exchange(...)`.

If it is pointing to a remote IP such as:

```java
"http://20.239.71.5:11434/api/generate"
```

change it to:

```java
"http://localhost:11434/api/generate"
```

This makes the backend talk to your local Ollama instance instead of a remote server.

### 7.4 Optional quick local check

You can test the local model directly:

```bash
ollama run phi3
```

If Ollama responds in the terminal, local model setup is working.

## 8) Run tests

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

## 9) Run CI-parity check against hosted Supabase

Create CI env file template:

```bash
cp .env.ci.example .env.ci
```

Fill `.env.ci` with:

- `CI_DB_URL_JDBC`
- `CI_DB_USER`
- `CI_DB_PASSWORD`
- `CI_SUPABASE_JWKS_URL`

Run CI-parity check:

```bash
./ci_check.sh
```

Windows:

```bat
ci_check.cmd
```

## Local endpoints

- Supabase Studio: `http://127.0.0.1:54323`
- Health: `http://localhost:8080/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`
