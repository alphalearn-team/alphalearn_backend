create table if not exists imposter_game_lobby_members (
    id bigserial primary key,
    lobby_id bigint not null,
    learner_id uuid not null,
    joined_at timestamptz not null default now(),
    constraint fk_imposter_game_lobby_members_lobby
        foreign key (lobby_id) references imposter_game_lobbies(id) on delete cascade,
    constraint fk_imposter_game_lobby_members_learner
        foreign key (learner_id) references learners(id) on delete cascade,
    constraint uk_imposter_game_lobby_members_lobby_learner unique (lobby_id, learner_id)
);

create index if not exists idx_imposter_game_lobby_members_lobby_id
    on imposter_game_lobby_members (lobby_id);

create index if not exists idx_imposter_game_lobby_members_learner_id
    on imposter_game_lobby_members (learner_id);
