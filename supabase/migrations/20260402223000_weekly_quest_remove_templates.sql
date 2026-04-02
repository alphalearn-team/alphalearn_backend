alter table if exists public.weekly_quest_assignments
    drop constraint if exists weekly_quest_assignments_quest_template_id_fkey;

alter table if exists public.weekly_quest_assignments
    drop column if exists quest_template_id;

drop table if exists public.quest_templates;
drop sequence if exists public.quest_templates_id_seq;

do $$
begin
    if exists (
        select 1
        from pg_type
        where typname = 'quest_submission_mode'
          and typnamespace = 'public'::regnamespace
    ) and not exists (
        select 1
        from pg_attribute a
        join pg_class c on c.oid = a.attrelid
        join pg_namespace n on n.oid = c.relnamespace
        join pg_type t on t.oid = a.atttypid
        where n.nspname = 'public'
          and t.typname = 'quest_submission_mode'
          and c.relkind = 'r'
          and a.attnum > 0
          and not a.attisdropped
    ) then
        drop type public.quest_submission_mode;
    end if;
end;
$$;
