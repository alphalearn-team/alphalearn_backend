# Local Development Guide

## Prerequisites

- Java 21
- Docker Desktop (running)
- Node.js (for `npx supabase`)

## Env Setup

Create local env files:

```bash
cp .env.shared.example .env.shared
cp .env.example .env.local
```

Fill `.env.local` with your local values.

## Run Backend

Default mode (`.env.shared + .env.local`):

```bash
./run_local.sh
```

Production-like mode (`.env.shared + .env.production`):

```bash
./run_local.sh production
```

## Run Tests

Default mode:

```bash
./test_local.sh
```

Production-like mode:

```bash
./test_local.sh production
```

## Local Endpoints

- Health: `http://localhost:8080/health`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

## Local Supabase

Start local stack:

```bash
npx supabase start
```

Check local services and keys:

```bash
npx supabase status
```
