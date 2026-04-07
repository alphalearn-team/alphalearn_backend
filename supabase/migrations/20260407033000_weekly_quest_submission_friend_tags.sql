create table if not exists public.weekly_quest_challenge_submission_tags (
    id bigserial primary key,
    submission_id bigint not null,
    tagged_learner_id uuid not null,
    created_at timestamptz not null default now(),
    constraint uk_weekly_quest_submission_tags_submission_learner
        unique (submission_id, tagged_learner_id),
    constraint fk_weekly_quest_submission_tags_submission
        foreign key (submission_id)
        references public.weekly_quest_challenge_submissions(id)
        on delete cascade,
    constraint fk_weekly_quest_submission_tags_tagged_learner
        foreign key (tagged_learner_id)
        references public.learners(id)
        on delete cascade
);

create index if not exists idx_weekly_quest_submission_tags_submission_id
    on public.weekly_quest_challenge_submission_tags (submission_id);

create index if not exists idx_weekly_quest_submission_tags_tagged_learner_id
    on public.weekly_quest_challenge_submission_tags (tagged_learner_id);

alter table if exists public.weekly_quest_challenge_submission_tags
    enable row level security;
