alter table if exists public.imposter_game_lobbies
    add column if not exists concept_count integer,
    add column if not exists rounds_per_concept integer,
    add column if not exists discussion_timer_seconds integer,
    add column if not exists imposter_guess_timer_seconds integer,
    add column if not exists current_concept_index integer,
    add column if not exists current_concept_public_id uuid,
    add column if not exists current_concept_title text,
    add column if not exists used_concept_public_ids text,
    add column if not exists current_imposter_learner_id uuid,
    add column if not exists current_phase varchar(32),
    add column if not exists round_number integer,
    add column if not exists round_drawer_order text,
    add column if not exists current_turn_index integer,
    add column if not exists current_drawer_learner_id uuid,
    add column if not exists turn_started_at timestamptz,
    add column if not exists turn_ends_at timestamptz,
    add column if not exists turn_completed_at timestamptz,
    add column if not exists round_completed_at timestamptz,
    add column if not exists turn_duration_seconds integer,
    add column if not exists current_drawing_snapshot text,
    add column if not exists drawing_version integer,
    add column if not exists voting_round_number integer,
    add column if not exists voting_eligible_target_learner_ids text,
    add column if not exists voting_ballots text,
    add column if not exists voting_deadline_at timestamptz,
    add column if not exists voted_out_learner_id uuid,
    add column if not exists imposter_guess_deadline_at timestamptz,
    add column if not exists last_imposter_guess text,
    add column if not exists last_imposter_guess_correct boolean,
    add column if not exists player_scores text,
    add column if not exists latest_result_concept_number integer,
    add column if not exists latest_result_concept_label text,
    add column if not exists latest_result_winner_side varchar(32),
    add column if not exists latest_result_resolution varchar(64),
    add column if not exists latest_result_accused_learner_id uuid,
    add column if not exists latest_result_imposter_learner_id uuid,
    add column if not exists latest_result_imposter_wins_by_voting_tie boolean,
    add column if not exists latest_result_imposter_guess text,
    add column if not exists latest_result_vote_tallies text,
    add column if not exists concept_result_deadline_at timestamptz,
    add column if not exists max_voting_rounds integer,
    add column if not exists state_version integer;
do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_imposter_game_lobbies_current_concept'
    ) then
        alter table public.imposter_game_lobbies
            add constraint fk_imposter_game_lobbies_current_concept
                foreign key (current_concept_public_id) references public.concepts(public_id);
    end if;
end
$$;
do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'fk_imposter_game_lobbies_current_imposter'
    ) then
        alter table public.imposter_game_lobbies
            add constraint fk_imposter_game_lobbies_current_imposter
                foreign key (current_imposter_learner_id) references public.learners(id);
    end if;
end
$$;
create index if not exists idx_imposter_game_lobbies_current_phase
    on public.imposter_game_lobbies (current_phase);
create index if not exists idx_imposter_game_lobbies_turn_ends_at
    on public.imposter_game_lobbies (turn_ends_at)
    where turn_ends_at is not null;
create index if not exists idx_imposter_game_lobbies_voting_deadline_at
    on public.imposter_game_lobbies (voting_deadline_at)
    where voting_deadline_at is not null;
create index if not exists idx_imposter_game_lobbies_guess_deadline_at
    on public.imposter_game_lobbies (imposter_guess_deadline_at)
    where imposter_guess_deadline_at is not null;
create index if not exists idx_imposter_game_lobbies_concept_result_deadline_at
    on public.imposter_game_lobbies (concept_result_deadline_at)
    where concept_result_deadline_at is not null;
