alter table if exists public.imposter_game_lobby_members
    add column if not exists removed_by_learner_id uuid,
    add column if not exists removed_reason varchar(64);

alter table if exists public.imposter_game_lobby_members
    add constraint fk_imposter_game_lobby_members_removed_by
        foreign key (removed_by_learner_id) references public.learners(id) on delete set null;
