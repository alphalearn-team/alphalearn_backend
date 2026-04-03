-- Local seed data (idempotent)
-- Applied during `npx supabase db reset` because config.toml has [db.seed].

begin;

-- Local admin auth user for development login:
-- email: admin@gmail.com
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
values (
  '00000000-0000-0000-0000-000000000000'::uuid,
  '8a7a21d2-f77e-4f55-8a9c-3d7d3b2b62cb'::uuid,
  'authenticated',
  'authenticated',
  'admin@gmail.com',
  crypt('123456', gen_salt('bf')),
  now(),
  now(),
  now(),
  '{"provider":"email","providers":["email"]}'::jsonb,
  '{}'::jsonb,
  false,
  '',
  '',
  '',
  ''
)
on conflict (id) do update
set
  email = excluded.email,
  encrypted_password = excluded.encrypted_password,
  email_confirmed_at = excluded.email_confirmed_at,
  updated_at = now(),
  raw_app_meta_data = excluded.raw_app_meta_data,
  raw_user_meta_data = excluded.raw_user_meta_data;

insert into public.admins (admin_id, created_at)
values ('8a7a21d2-f77e-4f55-8a9c-3d7d3b2b62cb'::uuid, now())
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


commit;
