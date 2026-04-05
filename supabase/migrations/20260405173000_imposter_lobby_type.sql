alter table if exists public.imposter_game_lobbies
    add column if not exists lobby_type varchar(32);

update public.imposter_game_lobbies
set lobby_type = 'PRIVATE_CUSTOM'
where lobby_type is null;

alter table if exists public.imposter_game_lobbies
    alter column lobby_type set not null;

alter table if exists public.imposter_game_lobbies
    drop constraint if exists ck_imposter_game_lobbies_private_only;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'ck_imposter_game_lobbies_lobby_type'
    ) then
        alter table public.imposter_game_lobbies
            add constraint ck_imposter_game_lobbies_lobby_type
                check (lobby_type in ('PRIVATE_CUSTOM', 'RANKED_MATCHMADE'));
    end if;
end
$$;

create index if not exists idx_imposter_game_lobbies_lobby_type
    on public.imposter_game_lobbies (lobby_type);
