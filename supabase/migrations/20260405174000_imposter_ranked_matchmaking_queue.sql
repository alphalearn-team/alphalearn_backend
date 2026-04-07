create table if not exists public.imposter_ranked_matchmaking_queue_entries (
    id bigserial primary key,
    learner_id uuid not null,
    status varchar(16) not null,
    queued_at timestamptz not null default now(),
    matched_at timestamptz,
    cancelled_at timestamptz,
    assigned_lobby_public_id uuid,
    constraint fk_imposter_ranked_queue_learner
        foreign key (learner_id) references public.learners(id) on delete cascade,
    constraint fk_imposter_ranked_queue_lobby
        foreign key (assigned_lobby_public_id) references public.imposter_game_lobbies(public_id) on delete set null,
    constraint ck_imposter_ranked_queue_status
        check (status in ('QUEUED', 'MATCHED', 'CANCELLED')),
    constraint ck_imposter_ranked_queue_matched_fields
        check (
            (status <> 'MATCHED')
            or
            (matched_at is not null and assigned_lobby_public_id is not null)
        ),
    constraint ck_imposter_ranked_queue_cancelled_fields
        check (
            (status <> 'CANCELLED')
            or
            (cancelled_at is not null)
        )
);

create index if not exists idx_imposter_ranked_queue_status
    on public.imposter_ranked_matchmaking_queue_entries (status, queued_at);

create index if not exists idx_imposter_ranked_queue_learner
    on public.imposter_ranked_matchmaking_queue_entries (learner_id);

create unique index if not exists uk_imposter_ranked_queue_active_learner
    on public.imposter_ranked_matchmaking_queue_entries (learner_id)
    where status = 'QUEUED';
