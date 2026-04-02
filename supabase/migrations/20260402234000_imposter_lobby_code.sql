alter table if exists public.imposter_game_lobbies
    add column if not exists lobby_code varchar(8);

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
    add constraint uk_imposter_game_lobbies_lobby_code unique (lobby_code);

alter table if exists public.imposter_game_lobbies
    add constraint ck_imposter_game_lobbies_lobby_code_format
        check (lobby_code ~ '^[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{8}$');

alter table if exists public.imposter_game_lobbies
    alter column lobby_code set not null;

drop function if exists public.generate_imposter_lobby_code();
