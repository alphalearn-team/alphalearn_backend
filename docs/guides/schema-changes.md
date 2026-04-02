# Schema Changes Guide (Supabase)

This project uses Supabase CLI migrations in `supabase/migrations`.

## One-Time Setup

1. Login:

```bash
npx supabase login
```

2. Link project:

```bash
npx supabase link --project-ref <project_ref>
```

Use your Supabase project ref from `https://<project_ref>.supabase.co`.

## Pull Existing Hosted Schema

If local is missing hosted tables:

```bash
npx supabase db pull remote_schema_init_all --schema public,auth,storage
npx supabase db reset
```

Use `db reset` here only when you intentionally want a full local rebuild.

## Apply Only New Migrations (Keep Existing Data)

Use this flow when you want to keep your local data and apply only pending migrations:

```bash
npx supabase start
npx supabase migration up
npx supabase migration list
```

`migration up` applies only migrations that are not yet recorded in migration history.

## Seed Data (Baseline Rows)

The name for "make sure some data is already in the DB" is **seeding**.

This repo uses:

- `supabase/seed.sql` for baseline local data (for example concepts, local bootstrap rows).
- `supabase/migrations/*.sql` for schema and durable data changes that should travel with migrations.

Run seed manually on current local DB (without reset):

```bash
psql postgresql://postgres:postgres@127.0.0.1:54322/postgres -f supabase/seed.sql
```

`db reset` will also run `seed.sql` automatically because `[db.seed]` is enabled in `supabase/config.toml`.

## Make Schema Changes Locally

### Option A: SQL-first

Create migration file:

```bash
npx supabase migration new add_lesson_flags
```

Edit the generated SQL under `supabase/migrations`.

Apply locally (choose one):

```bash
npx supabase migration up
```

or full rebuild:

```bash
npx supabase db reset
```

### Option B: Studio-first

1. Make table/column changes in local Studio (`http://127.0.0.1:54323`)
2. Generate migration diff:

```bash
npx supabase db diff -f add_lesson_flags
```

3. Rebuild local DB from migrations:

```bash
npx supabase db reset
```

If you want to keep existing local data instead, run:

```bash
npx supabase migration up
```

## Validate and Commit

Run backend/tests against the updated local DB:

```bash
./run_local.sh
./test_local.sh
```

Then commit migrations:

```bash
git add supabase/migrations
git commit -m "feat: add lesson flagging schema"
```

## Common Issue: Migration History Mismatch

If `db pull` reports remote migration mismatch:

1. Ensure branch is up-to-date.
2. Run:

```bash
npx supabase migration list
```

3. Repair specific IDs as needed:

```bash
npx supabase migration repair --status applied <migration_id>
```

## Manual SQL Apply Without Reset

If you already ran a migration SQL manually (Studio SQL editor or `psql`) and want Supabase CLI to treat it as applied:

```bash
npx supabase migration repair --status applied <migration_id>
```

Example:

```bash
npx supabase migration repair --status applied 20260402170000
```

This avoids re-running the same migration on the next `migration up`.

## RLS Default for New Tables

New tables created by migration SQL do not automatically enable Row Level Security. Add this explicitly in each migration:

```sql
alter table public.your_table enable row level security;
-- optional policy example
create policy "service_role_all"
on public.your_table
for all
to service_role
using (true)
with check (true);
```
