-- Seed friendships for Christoph (6666...) used by the friends feed.
-- Added one extra friend (Gabriel) so the feed can show three seeded posts.
insert into public.friends (user_id_1, user_id_2, created_at)
values
  ('11111111-1111-1111-1111-111111111111'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now()),
  ('44444444-4444-4444-4444-444444444444'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now()),
  ('55555555-5555-5555-5555-555555555555'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now()),
  ('22222222-2222-2222-2222-222222222222'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now())
on conflict (user_id_1, user_id_2) do nothing;

-- Seed only submissions for the existing Cringe weekly quest assignment.
-- Pin current week to Cringe for demo stability and seed submissions against it.
with current_week as (
  select (
    date_trunc('day', timezone('Asia/Singapore', now()))
    - (extract(dow from timezone('Asia/Singapore', now()))::int * interval '1 day')
  )::timestamptz as week_start_at
),
upsert_week as (
  insert into public.weekly_quest_weeks (
    public_id,
    week_start_at,
    setup_deadline_at,
    status,
    activation_source,
    activated_at,
    created_at
  )
  select
    gen_random_uuid(),
    cw.week_start_at,
    cw.week_start_at - interval '7 days',
    'ACTIVE'::public.weekly_quest_week_status,
    'ADMIN'::public.weekly_quest_activation_source,
    now(),
    now()
  from current_week cw
  on conflict (week_start_at) do update
  set
    status = 'ACTIVE'::public.weekly_quest_week_status,
    activation_source = 'ADMIN'::public.weekly_quest_activation_source,
    activated_at = coalesce(public.weekly_quest_weeks.activated_at, now())
  returning id
),
cringe_concept as (
  select concept_id
  from public.concepts
  where public_id = '3f125211-fe25-46e8-9f8a-2d51725b98fa'::uuid
  limit 1
),
upsert_assignment as (
  insert into public.weekly_quest_assignments (
    public_id,
    week_id,
    concept_id,
    slot_index,
    is_official,
    source_type,
    status,
    created_by_admin_id,
    created_at,
    updated_at
  )
  select
    gen_random_uuid(),
    uw.id,
    cc.concept_id,
    0,
    true,
    'ADMIN'::public.weekly_quest_assignment_source_type,
    'ACTIVE'::public.weekly_quest_assignment_status,
    null,
    now(),
    now()
  from upsert_week uw
  cross join cringe_concept cc
  on conflict (week_id) where is_official = true do update
  set
    concept_id = excluded.concept_id,
    source_type = 'ADMIN'::public.weekly_quest_assignment_source_type,
    status = 'ACTIVE'::public.weekly_quest_assignment_status,
    updated_at = now()
  returning id
),
target_assignment as (
  select id as assignment_id
  from upsert_assignment
),
seed_posts as (
  select
    '44444444-4444-4444-4444-444444444444'::uuid as learner_id,
    'quest-submissions/44444444/cringe quest2.png'::text as media_object_key,
    'https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/74f67afb-2998-4213-b2e7-dc2284829ba8/66666666-6666-6666-6666-666666666666/d729e7db-deef-4d44-949c-97cf4b543df3-cringe-quest2.png'::text as media_public_url,
    'image/png'::text as media_content_type,
    'cringe quest2.png'::text as original_filename,
    238412::bigint as file_size_bytes,
    'Apparently, I panic too much in text'::text as caption,
    now() - interval '5 hours' as submitted_at
  union all
  select
    '22222222-2222-2222-2222-222222222222'::uuid,
    'quest-submissions/22222222/cringe quest1.jpg'::text,
    'https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/aef90f84-45da-4e8a-b505-bebdf4e8fd3c/55555555-5555-5555-5555-555555555555/023443ec-99d2-4007-9b0f-06974f5c15a2-cringe-quest1.jpg'::text,
    'image/jpeg'::text,
    'cringe quest1.jpg'::text,
    15545::bigint,
    'I think I understood what being cringe means now 🥹'::text,
    now() - interval '3 hours'
  union all
  select
    '55555555-5555-5555-5555-555555555555'::uuid,
    'quest-submissions/55555555/quest cringe 3.mp4'::text,
    'https://pub-6ae6c44a993a415fb6d112bbab13f0fc.r2.dev/quest-challenges/74f67afb-2998-4213-b2e7-dc2284829ba8/11111111-1111-1111-1111-111111111111/6ddca00e-567d-46d0-bcae-da2d134a1405-quest-cringe-3.mp4'::text,
    'video/mp4'::text,
    'quest cringe 3.mp4'::text,
    1339979::bigint,
    'Accidentally waved back at someone who was waving to the person behind me.'::text,
    now() - interval '1 hour'
),
upserted_submissions as (
  insert into public.weekly_quest_challenge_submissions (
    public_id,
    learner_id,
    weekly_quest_assignment_id,
    media_object_key,
    media_public_url,
    media_content_type,
    original_filename,
    file_size_bytes,
    caption,
    submitted_at,
    updated_at,
    visibility
  )
  select
    gen_random_uuid(),
    p.learner_id,
    a.assignment_id,
    p.media_object_key,
    p.media_public_url,
    p.media_content_type,
    p.original_filename,
    p.file_size_bytes,
    p.caption,
    p.submitted_at,
    now(),
    'FRIENDS'::public.quest_submission_visibility
  from seed_posts p
  cross join target_assignment a
  on conflict (learner_id, weekly_quest_assignment_id) do update
  set
    media_object_key = excluded.media_object_key,
    media_public_url = excluded.media_public_url,
    media_content_type = excluded.media_content_type,
    original_filename = excluded.original_filename,
    file_size_bytes = excluded.file_size_bytes,
    caption = excluded.caption,
    submitted_at = excluded.submitted_at,
    updated_at = now(),
    visibility = excluded.visibility
  returning id, learner_id
),
seed_tags as (
  -- owner_learner_id => tagged_learner_id
  select *
  from (values
    ('44444444-4444-4444-4444-444444444444'::uuid, '55555555-5555-5555-5555-555555555555'::uuid),
    ('22222222-2222-2222-2222-222222222222'::uuid, '44444444-4444-4444-4444-444444444444'::uuid),
    ('55555555-5555-5555-5555-555555555555'::uuid, '22222222-2222-2222-2222-222222222222'::uuid)
  ) as t(owner_learner_id, tagged_learner_id)
)
insert into public.weekly_quest_challenge_submission_tags (submission_id, tagged_learner_id, created_at)
select
  u.id,
  st.tagged_learner_id,
  now()
from upserted_submissions u
join seed_tags st on st.owner_learner_id = u.learner_id
on conflict (submission_id, tagged_learner_id) do nothing;
