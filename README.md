# Backend Docs

## Guides

- Local setup and run: [docs/guides/local-development.md](docs/guides/local-development.md)
- Supabase schema changes and migrations: [docs/guides/schema-changes.md](docs/guides/schema-changes.md)
- Supabase CLI install (Windows + macOS): [docs/guides/supabase-cli-install.md](docs/guides/supabase-cli-install.md)

## Seeded Demo Accounts

These accounts are seeded by:
- Local reset seed: `supabase/seed.sql`
- Migration seed (CI/prod): `supabase/migrations/20260405083000_seed_demo_accounts_concepts_and_april_pack.sql`

Shared password for all seeded accounts: `123456`

Contributors:
- `contributor.gabriel@gmail.com`
- `contributor.jeniffer@gmail.com`
- `contributor.josh@gmail.com`

Learners:
- `learner.nathaniel@gmail.com`
- `learner.engkit@gmail.com`
- `learner.christoph@gmail.com`

Admin:
- `admin.jiugeng@gmail.com`

Security note: these are seeded/demo credentials and should be rotated or removed for public production environments.

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
