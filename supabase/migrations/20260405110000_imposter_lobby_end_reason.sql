alter table if exists public.imposter_game_lobbies
    add column if not exists ended_reason varchar(64);
