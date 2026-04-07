alter table if exists public.imposter_game_lobbies
    add column if not exists started_at timestamptz,
    add column if not exists started_by_learner_id uuid;
alter table if exists public.imposter_game_lobbies
    add constraint fk_imposter_game_lobbies_started_by_learner
        foreign key (started_by_learner_id) references public.learners(id);
alter table if exists public.imposter_game_lobbies
    add constraint ck_imposter_game_lobbies_started_columns_pair
        check (
            (started_at is null and started_by_learner_id is null)
            or
            (started_at is not null and started_by_learner_id is not null)
        );
create index if not exists idx_imposter_game_lobbies_started_at
    on public.imposter_game_lobbies (started_at);
