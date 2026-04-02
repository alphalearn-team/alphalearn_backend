## Local Spring Boot Run

### 1. Create local env files

Create/confirm:

- `.env.shared` (shared settings, local-only, ignored by git)
- `.env.local` (local secrets, ignored by git)

If needed:

```bash
cp .env.shared.example .env.shared
cp .env.example .env.local
```

### 2. Run backend

Preferred (uses layered env loading):

```bash
./run_local.sh
```

Optional modes:

```bash
./run_local.sh production
```

Open:

- Backend root health check: http://localhost:8080/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

### 3. Run tests with same env layering

```bash
./test_local.sh
```

Optional modes:

```bash
./test_local.sh production
```

## CI/CD Pipeline

### Backend CI

GitHub Actions workflow: `.github/workflows/backend-ci.yml`

On pull requests and pushes to `main`, CI will:

1. Check out the repository.
2. Set up Java 21 with Maven caching.
3. Run `mvn -B clean verify`.
4. Build the backend Docker image to verify the container path still works.

### Backend CD

GitHub Actions workflow: `.github/workflows/backend-cd.yml`

On pushes to `main`, CD will:

1. Check out the repository.
2. Authenticate to AWS.
3. Log in to Amazon ECR.
4. Build and push the backend image with both commit-SHA and `latest` tags.
5. Trigger an AWS App Runner deployment.
