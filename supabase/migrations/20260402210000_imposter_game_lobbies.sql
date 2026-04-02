create table if not exists imposter_game_lobbies (
    id bigserial primary key,
    public_id uuid not null default gen_random_uuid(),
    host_learner_id uuid not null,
    is_private boolean not null default true,
    concept_pool_mode varchar(32) not null,
    pinned_year_month varchar(7),
    created_at timestamptz not null default now(),
    constraint uk_imposter_game_lobbies_public_id unique (public_id),
    constraint fk_imposter_game_lobbies_host_learner
        foreign key (host_learner_id) references learners(id),
    constraint ck_imposter_game_lobbies_private_only check (is_private = true),
    constraint ck_imposter_game_lobbies_pool_mode
        check (concept_pool_mode in ('CURRENT_MONTH_PACK', 'FULL_CONCEPT_POOL')),
    constraint ck_imposter_game_lobbies_pinned_year_month
        check (
            (concept_pool_mode = 'CURRENT_MONTH_PACK' and pinned_year_month is not null and pinned_year_month ~ '^[0-9]{4}-[0-9]{2}$')
            or
            (concept_pool_mode = 'FULL_CONCEPT_POOL' and pinned_year_month is null)
        )
);
