## Local Spring Boot Run

On Windows:

```bash
.\mvnw spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Open:

- Backend root health check: http://localhost:8080/health
- Swagger UI: http://localhost:8080/swagger-ui/index.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Dockerized Backend

This backend can be built and run as a Docker container while continuing to use
the existing Supabase/Postgres database.

### 1. Create the runtime env file

Copy the example file and fill in real values:

```bash
cp docker/backend.env.example docker/backend.env
```

Required values:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SUPABASE_JWT_JWKS_URL`
- `APP_CORS_ALLOWED_ORIGINS`

Do not commit `docker/backend.env`.

### 2. Build the Docker image

From the backend repo root:

```bash
docker build -f docker/backend.Dockerfile -t alphalearn-backend .
```

### 3. Run the backend container

```bash
docker run --rm --env-file docker/backend.env -p 8080:8080 alphalearn-backend
```

### 4. Smoke-test the container

Check:

- `http://localhost:8080/health`
- `http://localhost:8080/swagger-ui/index.html`
- `http://localhost:8080/v3/api-docs`

If startup fails, verify the JDBC URL, database credentials, and Supabase JWKS
URL first.

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

## AWS Configuration

### GitHub secrets

- `AWS_ACCESS_KEY_ID`
- `AWS_SECRET_ACCESS_KEY`
- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`

### GitHub variables

- `AWS_REGION`
- `ECR_REPOSITORY_URI`
- `APP_RUNNER_SERVICE_ARN`
- `SUPABASE_JWT_JWKS_URL`

### App Runner runtime environment variables

Configure these directly in App Runner:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SUPABASE_JWT_JWKS_URL`
- `APP_CORS_ALLOWED_ORIGINS`
- Optional Cloudflare R2 values if upload features are needed

## Deployment Strategy

This backend uses:

- Docker for reproducible packaging
- GitHub Actions for CI and CD
- Amazon ECR as the image registry
- AWS App Runner as the deployment target
- Supabase/Postgres as the current managed database

Frontend deployment stays separate from the backend deployment flow.
