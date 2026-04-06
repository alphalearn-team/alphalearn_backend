-- Forward migration to revert ranked matchmaking schema and return to pre-ranked lobby model.

-- Remove ranked lobbies (and cascaded memberships) before dropping lobby_type semantics.
delete from public.imposter_game_lobbies
where lobby_type = 'RANKED_MATCHMADE';

-- Ranked queue table is no longer needed.
drop table if exists public.imposter_ranked_matchmaking_queue_entries;

-- Remove ranked-era constraints/indexes.
alter table if exists public.imposter_game_lobbies
    drop constraint if exists ck_imposter_game_lobbies_lobby_type;

drop index if exists public.idx_imposter_game_lobbies_lobby_type;
drop index if exists public.uk_imposter_game_lobbies_lobby_code;

alter table if exists public.imposter_game_lobbies
    drop constraint if exists ck_imposter_game_lobbies_lobby_code_format;

-- Ensure all remaining lobbies have a valid invite code before enforcing NOT NULL + global uniqueness.
create or replace function public.generate_imposter_lobby_code()
returns text
language plpgsql
as $$
declare
    chars constant text := 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789';
    result text := '';
    idx int;
begin
    for i in 1..8 loop
        idx := floor(random() * length(chars))::int + 1;
        result := result || substr(chars, idx, 1);
    end loop;
    return result;
end;
$$;

do $$
declare
    row_record record;
    candidate text;
begin
    for row_record in
        select id
        from public.imposter_game_lobbies
        where lobby_code is null
    loop
        loop
            candidate := public.generate_imposter_lobby_code();
            exit when not exists (
                select 1
                from public.imposter_game_lobbies
                where lobby_code = candidate
            );
        end loop;

        update public.imposter_game_lobbies
        set lobby_code = candidate
        where id = row_record.id;
    end loop;
end;
$$;

alter table if exists public.imposter_game_lobbies
    alter column lobby_code set not null;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uk_imposter_game_lobbies_lobby_code'
    ) then
        alter table public.imposter_game_lobbies
            add constraint uk_imposter_game_lobbies_lobby_code unique (lobby_code);
    end if;
end
$$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'ck_imposter_game_lobbies_lobby_code_format'
    ) then
        alter table public.imposter_game_lobbies
            add constraint ck_imposter_game_lobbies_lobby_code_format
                check (lobby_code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$');
    end if;
end
$$;

do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'ck_imposter_game_lobbies_private_only'
    ) then
        alter table public.imposter_game_lobbies
            add constraint ck_imposter_game_lobbies_private_only check (is_private = true);
    end if;
end
$$;

alter table if exists public.imposter_game_lobbies
    drop column if exists lobby_type;

drop function if exists public.generate_imposter_lobby_code();
