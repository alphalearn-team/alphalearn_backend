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

## Make Schema Changes Locally

### Option A: SQL-first

Create migration file:

```bash
npx supabase migration new add_lesson_flags
```

Edit the generated SQL under `supabase/migrations`.

Apply locally:

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
