alter table if exists public.imposter_game_lobbies
    add column if not exists ended_at timestamptz,
    add column if not exists abandoned_by_learner_id uuid;

create index if not exists idx_imposter_game_lobbies_ended_at
    on public.imposter_game_lobbies (ended_at)
    where ended_at is not null;
