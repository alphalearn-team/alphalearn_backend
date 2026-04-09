create table if not exists imposter_game_lobby_invites (
    id bigserial primary key,
    public_id uuid not null default gen_random_uuid(),
    lobby_id bigint not null,
    sender_learner_id uuid not null,
    receiver_learner_id uuid not null,
    status varchar(16) not null,
    created_at timestamptz not null default now(),
    responded_at timestamptz,
    expires_at timestamptz,
    constraint uk_imposter_game_lobby_invites_public_id unique (public_id),
    constraint fk_imposter_game_lobby_invites_lobby
        foreign key (lobby_id) references imposter_game_lobbies(id) on delete cascade,
    constraint fk_imposter_game_lobby_invites_sender
        foreign key (sender_learner_id) references learners(id) on delete cascade,
    constraint fk_imposter_game_lobby_invites_receiver
        foreign key (receiver_learner_id) references learners(id) on delete cascade,
    constraint ck_imposter_game_lobby_invites_status
        check (status in ('PENDING', 'ACCEPTED', 'REJECTED', 'EXPIRED', 'CANCELED')),
    constraint ck_imposter_game_lobby_invites_sender_receiver
        check (sender_learner_id <> receiver_learner_id)
);

create index if not exists idx_imposter_game_lobby_invites_receiver_status_created
    on imposter_game_lobby_invites (receiver_learner_id, status, created_at desc);

create index if not exists idx_imposter_game_lobby_invites_sender_status_created
    on imposter_game_lobby_invites (sender_learner_id, status, created_at desc);

create unique index if not exists uk_imposter_game_lobby_invites_pending_lobby_receiver
    on imposter_game_lobby_invites (lobby_id, receiver_learner_id)
    where status = 'PENDING';

alter table if exists notifications
    add column if not exists type varchar(64) not null default 'GENERIC';

alter table if exists notifications
    add column if not exists metadata_json text;
