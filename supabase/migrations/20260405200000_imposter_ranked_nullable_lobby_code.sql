alter table if exists public.imposter_game_lobbies
    drop constraint if exists uk_imposter_game_lobbies_lobby_code;

alter table if exists public.imposter_game_lobbies
    drop constraint if exists ck_imposter_game_lobbies_lobby_code_format;

drop index if exists public.uk_imposter_game_lobbies_lobby_code;

alter table if exists public.imposter_game_lobbies
    alter column lobby_code drop not null;

update public.imposter_game_lobbies
set lobby_code = null
where lobby_type = 'RANKED_MATCHMADE';

create unique index if not exists uk_imposter_game_lobbies_lobby_code
    on public.imposter_game_lobbies (lobby_code)
    where lobby_type = 'PRIVATE_CUSTOM'
      and lobby_code is not null;

alter table if exists public.imposter_game_lobbies
    add constraint ck_imposter_game_lobbies_lobby_code_format
        check (
            (lobby_type = 'PRIVATE_CUSTOM'
                and lobby_code is not null
                and lobby_code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$')
            or
            (lobby_type = 'RANKED_MATCHMADE' and lobby_code is null)
        );
