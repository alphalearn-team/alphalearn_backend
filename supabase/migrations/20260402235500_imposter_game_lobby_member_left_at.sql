alter table if exists public.imposter_game_lobby_members
    add column if not exists left_at timestamptz;
alter table if exists public.imposter_game_lobby_members
    add constraint ck_imposter_game_lobby_members_left_after_join
        check (left_at is null or left_at >= joined_at);
create index if not exists idx_imposter_game_lobby_members_active
    on public.imposter_game_lobby_members (lobby_id, learner_id)
    where left_at is null;
