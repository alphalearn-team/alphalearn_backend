do $$
begin
    if not exists (
        select 1
        from pg_type
        where typname = 'lesson_report_status'
          and typnamespace = 'public'::regnamespace
    ) then
        create type public.lesson_report_status as enum ('PENDING', 'RESOLVED');
    end if;
end;
$$;

do $$
begin
    if not exists (
        select 1
        from pg_type
        where typname = 'lesson_report_resolution_action'
          and typnamespace = 'public'::regnamespace
    ) then
        create type public.lesson_report_resolution_action as enum ('DISMISSED', 'UNPUBLISHED');
    end if;
end;
$$;

alter table if exists public.lesson_reports
    add column if not exists status public.lesson_report_status not null default 'PENDING';

alter table if exists public.lesson_reports
    add column if not exists resolved_at timestamptz;

alter table if exists public.lesson_reports
    add column if not exists resolved_by_admin_user_id uuid;

alter table if exists public.lesson_reports
    add column if not exists resolution_action public.lesson_report_resolution_action;

create index if not exists idx_lesson_reports_status
    on public.lesson_reports (status);

create index if not exists idx_lesson_reports_lesson_id_status
    on public.lesson_reports (lesson_id, status);

create index if not exists idx_lesson_reports_created_at
    on public.lesson_reports (created_at desc);
