# Backend Docs

## Guides

- Local setup and run: [docs/guides/local-development.md](docs/guides/local-development.md)
- Supabase schema changes and migrations: [docs/guides/schema-changes.md](docs/guides/schema-changes.md)
- Supabase CLI install (Windows + macOS): [docs/guides/supabase-cli-install.md](docs/guides/supabase-cli-install.md)

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
