create table if not exists public.lesson_reports (
    lesson_report_id bigserial primary key,
    public_id uuid not null default gen_random_uuid(),
    lesson_id integer not null,
    reporter_user_id uuid not null,
    reason text not null,
    created_at timestamptz not null default now(),
    constraint uk_lesson_reports_public_id unique (public_id),
    constraint uk_lesson_reports_lesson_id_reporter_user_id unique (lesson_id, reporter_user_id),
    constraint fk_lesson_reports_lesson_id
        foreign key (lesson_id) references public.lessons(lesson_id) on delete cascade
);

create index if not exists idx_lesson_reports_lesson_id
    on public.lesson_reports (lesson_id);

create index if not exists idx_lesson_reports_reporter_user_id
    on public.lesson_reports (reporter_user_id);

drop table if exists public.flags;
drop type if exists public.flag_content_type;
