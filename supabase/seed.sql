-- Local seed data (idempotent)
-- Applied during `npx supabase db reset` because config.toml has [db.seed].

begin;

-- Local seeded auth users for development login:
-- password for all seeded users: 123456
-- contributors: gabriel, jeniffer, josh
-- learners: nathaniel, eng_kit, christoph
-- admin: jiugeng
-- password: 123456
insert into auth.users (
  instance_id,
  id,
  aud,
  role,
  email,
  encrypted_password,
  email_confirmed_at,
  created_at,
  updated_at,
  raw_app_meta_data,
  raw_user_meta_data,
  is_super_admin,
  confirmation_token,
  email_change,
  email_change_token_new,
  recovery_token
)
select
  '00000000-0000-0000-0000-000000000000'::uuid,
  su.id,
  'authenticated',
  'authenticated',
  su.email,
  crypt('123456', gen_salt('bf')),
  now(),
  now(),
  now(),
  '{"provider":"email","providers":["email"]}'::jsonb,
  jsonb_build_object('role_label', su.role_label),
  false,
  '',
  '',
  '',
  ''
from (
  values
    ('11111111-1111-1111-1111-111111111111'::uuid, 'contributor.gabriel@gmail.com',  'CONTRIBUTOR'),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'contributor.jeniffer@gmail.com', 'CONTRIBUTOR'),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'contributor.josh@gmail.com',     'CONTRIBUTOR'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'learner.nathaniel@gmail.com',    'LEARNER'),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'learner.engkit@gmail.com',       'LEARNER'),
    ('66666666-6666-6666-6666-666666666666'::uuid, 'learner.christoph@gmail.com',    'LEARNER'),
    ('77777777-7777-7777-7777-777777777777'::uuid, 'admin.jiugeng@gmail.com',        'ADMIN')
) as su(id, email, role_label)
on conflict (email) where (is_sso_user = false) do update
set
  id = excluded.id,
  email = excluded.email,
  encrypted_password = excluded.encrypted_password,
  email_confirmed_at = excluded.email_confirmed_at,
  updated_at = now(),
  raw_app_meta_data = excluded.raw_app_meta_data,
  raw_user_meta_data = excluded.raw_user_meta_data;

insert into auth.identities (
  user_id,
  provider_id,
  identity_data,
  provider,
  created_at,
  updated_at
)
select
  su.id,
  su.id::text,
  jsonb_build_object(
    'sub', su.id::text,
    'email', su.email,
    'email_verified', true,
    'phone_verified', false
  ),
  'email',
  now(),
  now()
from (
  values
    ('11111111-1111-1111-1111-111111111111'::uuid, 'contributor.gabriel@gmail.com'),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'contributor.jeniffer@gmail.com'),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'contributor.josh@gmail.com'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'learner.nathaniel@gmail.com'),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'learner.engkit@gmail.com'),
    ('66666666-6666-6666-6666-666666666666'::uuid, 'learner.christoph@gmail.com'),
    ('77777777-7777-7777-7777-777777777777'::uuid, 'admin.jiugeng@gmail.com')
) as su(id, email)
on conflict (provider_id, provider) do update
set
  user_id = excluded.user_id,
  identity_data = excluded.identity_data,
  updated_at = now();

insert into public.learners (id, username, created_at, total_points, public_id, bio, profile_picture)
select
  su.id,
  su.username,
  now(),
  0,
  su.public_id,
  null,
  null
from (
  values
    ('11111111-1111-1111-1111-111111111111'::uuid, 'gabriel',   'CONTRIBUTOR', 'a1111111-1111-4111-8111-111111111111'::uuid),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'jeniffer',  'CONTRIBUTOR', 'a2222222-2222-4222-8222-222222222222'::uuid),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'josh',      'CONTRIBUTOR', 'a3333333-3333-4333-8333-333333333333'::uuid),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'nathaniel', 'LEARNER',     'a4444444-4444-4444-8444-444444444444'::uuid),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'eng_kit',   'LEARNER',     'a5555555-5555-4555-8555-555555555555'::uuid),
    ('66666666-6666-6666-6666-666666666666'::uuid, 'christoph', 'LEARNER',     'a6666666-6666-4666-8666-666666666666'::uuid),
    ('77777777-7777-7777-7777-777777777777'::uuid, 'jiugeng',   'ADMIN',       'a7777777-7777-4777-8777-777777777777'::uuid)
) as su(id, username, role_label, public_id)
where su.role_label <> 'ADMIN'
on conflict (id) do update
set
  username = excluded.username,
  total_points = excluded.total_points,
  public_id = excluded.public_id,
  bio = excluded.bio,
  profile_picture = excluded.profile_picture;

insert into public.contributors (contributor_id, promoted_at, total_points, demoted_at)
select
  su.id,
  now(),
  0,
  null
from (
  values
    ('11111111-1111-1111-1111-111111111111'::uuid, 'CONTRIBUTOR'),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'CONTRIBUTOR'),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'CONTRIBUTOR'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'LEARNER'),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'LEARNER'),
    ('66666666-6666-6666-6666-666666666666'::uuid, 'LEARNER'),
    ('77777777-7777-7777-7777-777777777777'::uuid, 'ADMIN')
) as su(id, role_label)
where su.role_label = 'CONTRIBUTOR'
on conflict (contributor_id) do update
set
  total_points = excluded.total_points,
  demoted_at = excluded.demoted_at;

insert into public.admins (admin_id, created_at)
select su.id, now()
from (
  values
    ('11111111-1111-1111-1111-111111111111'::uuid, 'CONTRIBUTOR'),
    ('22222222-2222-2222-2222-222222222222'::uuid, 'CONTRIBUTOR'),
    ('33333333-3333-3333-3333-333333333333'::uuid, 'CONTRIBUTOR'),
    ('44444444-4444-4444-4444-444444444444'::uuid, 'LEARNER'),
    ('55555555-5555-5555-5555-555555555555'::uuid, 'LEARNER'),
    ('66666666-6666-6666-6666-666666666666'::uuid, 'LEARNER'),
    ('77777777-7777-7777-7777-777777777777'::uuid, 'ADMIN')
) as su(id, role_label)
where su.role_label = 'ADMIN'
on conflict (admin_id) do update
set created_at = admins.created_at;

insert into public.concepts (public_id, title, description, created_at)
values
  ('3f125211-fe25-46e8-9f8a-2d51725b98fa'::uuid, 'Cringe', 'Cringe means to feel extreme embarrassment, awkwardness, or discomfort, often accompanied by a physical reaction like wincing or pulling back', now()),
  ('f1c3c556-7ba5-4d4d-8aa8-218f9d3305b7'::uuid, 'Skibidi', 'A nonsense-style word used for chaotic, random, or absurd situations.', now()),
  ('aa85aada-754b-452c-907d-12d34cba956f'::uuid, 'Rizz', 'Charm or flirting skill, especially social confidence with others.', now()),
  ('665371ce-d5ea-4f1c-95b7-f10148bb2cad'::uuid, 'Sigma', 'A self-reliant lone wolf archetype outside normal social hierarchy.', now()),
  ('0d070bb1-5273-4772-b08e-6c0887642549'::uuid, 'Gyatt', 'An exaggerated reaction word, usually expressing surprise at someone''s looks.', now()),
  ('2f3c5429-403e-4e29-b6fc-ad75c24ba52f'::uuid, 'Fanum Tax', 'Jokingly taxing your friend by taking some of their food.', now()),
  ('1c67a384-e51f-48cd-9843-2372fa3ded25'::uuid, 'Mewing', 'A jaw/posture trend said to improve facial structure over time.', now()),
  ('e0890e7b-d4ec-44eb-a237-ca9d0d66daad'::uuid, 'Looksmaxxing', 'Trying to maximize appearance through grooming, style, and routines.', now()),
  ('2b89f138-0db6-4bf7-a7cc-8f83e1103b38'::uuid, 'Delulu', 'Slang for being unrealistically optimistic or delusional in a funny way.', now()),
  ('f8d95082-4757-410f-acaf-856c810f8920'::uuid, 'Cooked', 'Completely done for, exhausted, or in a bad situation.', now()),
  ('54961687-39c3-44ed-8d89-1e12f39ddf95'::uuid, 'Let Him Cook', 'Let someone continue what they are doing because it might work out.', now()),
  ('993c6e34-39bc-4897-b003-0cb3c31f598b'::uuid, 'Bussin', 'Extremely good, usually used for food.', now()),
  ('3e61030c-925b-4743-a849-5afd1f05de44'::uuid, 'No Cap', 'No lie or for real.', now()),
  ('d8af6537-1179-44df-b3f3-cb2b9c907bd5'::uuid, 'Bet', 'Agreement or confirmation, like okay or deal.', now()),
  ('dfd66891-c298-49de-aaae-a982db9c5f7f'::uuid, 'Mid', 'Average or underwhelming quality.', now()),
  ('58b57a1f-5d70-4ad5-a2a9-e5095a2ca146'::uuid, 'NPC', 'Someone acting generic, repetitive, or lacking original thought.', now()),
  ('3d1dbb30-ef54-47fe-80c2-40b67ef4e472'::uuid, 'Aura', 'Perceived vibe, coolness, or social presence someone gives off.', now()),
  ('9b40e112-0270-4dfc-bcb8-9c062352eac7'::uuid, 'W / L', 'Win or loss; shorthand for judging outcomes or behavior.', now()),
  ('09f4ecd3-48e1-4cff-a55f-2ee3a383d3e2'::uuid, 'Glazing', 'Overpraising someone excessively (like overhyping).', now()),
  ('656cf9fc-531f-40a8-8801-87eb49e27d90'::uuid, 'Brainrot', 'Internet content that is so repetitive it takes over your thoughts/humor.', now())
on conflict (public_id) do update
set
  title = excluded.title,
  description = excluded.description;

-- April 2026 imposter monthly pack: all 20 concepts, with 4 weekly featured slots.
with upsert_pack as (
  insert into public.imposter_monthly_packs (year_month)
  values ('2026-04')
  on conflict (year_month) do update
    set year_month = excluded.year_month
  returning id
),
pack as (
  select id from upsert_pack
  union all
  select p.id
  from public.imposter_monthly_packs p
  where p.year_month = '2026-04'
  limit 1
)
delete from public.imposter_monthly_pack_weekly_features wf
using pack
where wf.pack_id = pack.id;

with upsert_pack as (
  insert into public.imposter_monthly_packs (year_month)
  values ('2026-04')
  on conflict (year_month) do update
    set year_month = excluded.year_month
  returning id
),
pack as (
  select id from upsert_pack
  union all
  select p.id
  from public.imposter_monthly_packs p
  where p.year_month = '2026-04'
  limit 1
)
delete from public.imposter_monthly_pack_concepts pc
using pack
where pc.pack_id = pack.id;

with upsert_pack as (
  insert into public.imposter_monthly_packs (year_month)
  values ('2026-04')
  on conflict (year_month) do update
    set year_month = excluded.year_month
  returning id
),
pack as (
  select id from upsert_pack
  union all
  select p.id
  from public.imposter_monthly_packs p
  where p.year_month = '2026-04'
  limit 1
),
pack_concepts(public_id, slot_index) as (
  values
    ('3f125211-fe25-46e8-9f8a-2d51725b98fa'::uuid, 1),
    ('f1c3c556-7ba5-4d4d-8aa8-218f9d3305b7'::uuid, 2),
    ('aa85aada-754b-452c-907d-12d34cba956f'::uuid, 3),
    ('665371ce-d5ea-4f1c-95b7-f10148bb2cad'::uuid, 4),
    ('0d070bb1-5273-4772-b08e-6c0887642549'::uuid, 5),
    ('2f3c5429-403e-4e29-b6fc-ad75c24ba52f'::uuid, 6),
    ('1c67a384-e51f-48cd-9843-2372fa3ded25'::uuid, 7),
    ('e0890e7b-d4ec-44eb-a237-ca9d0d66daad'::uuid, 8),
    ('2b89f138-0db6-4bf7-a7cc-8f83e1103b38'::uuid, 9),
    ('f8d95082-4757-410f-acaf-856c810f8920'::uuid, 10),
    ('54961687-39c3-44ed-8d89-1e12f39ddf95'::uuid, 11),
    ('993c6e34-39bc-4897-b003-0cb3c31f598b'::uuid, 12),
    ('3e61030c-925b-4743-a849-5afd1f05de44'::uuid, 13),
    ('d8af6537-1179-44df-b3f3-cb2b9c907bd5'::uuid, 14),
    ('dfd66891-c298-49de-aaae-a982db9c5f7f'::uuid, 15),
    ('58b57a1f-5d70-4ad5-a2a9-e5095a2ca146'::uuid, 16),
    ('3d1dbb30-ef54-47fe-80c2-40b67ef4e472'::uuid, 17),
    ('9b40e112-0270-4dfc-bcb8-9c062352eac7'::uuid, 18),
    ('09f4ecd3-48e1-4cff-a55f-2ee3a383d3e2'::uuid, 19),
    ('656cf9fc-531f-40a8-8801-87eb49e27d90'::uuid, 20)
)
insert into public.imposter_monthly_pack_concepts (pack_id, concept_id, slot_index)
select
  pack.id,
  c.concept_id,
  pc.slot_index
from pack
join pack_concepts pc on true
join public.concepts c on c.public_id = pc.public_id;

with upsert_pack as (
  insert into public.imposter_monthly_packs (year_month)
  values ('2026-04')
  on conflict (year_month) do update
    set year_month = excluded.year_month
  returning id
),
pack as (
  select id from upsert_pack
  union all
  select p.id
  from public.imposter_monthly_packs p
  where p.year_month = '2026-04'
  limit 1
)
insert into public.imposter_monthly_pack_weekly_features (pack_id, concept_id, week_slot)
select
  pmc.pack_id,
  pmc.concept_id,
  case pmc.slot_index
    when 1 then 1
    when 7 then 2
    when 13 then 3
    when 19 then 4
  end as week_slot
from public.imposter_monthly_pack_concepts pmc
join pack on pack.id = pmc.pack_id
where pmc.slot_index in (1, 7, 13, 19);

commit;
