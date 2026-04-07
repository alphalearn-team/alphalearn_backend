insert into public.friends (user_id_1, user_id_2, created_at)
values
  ('44444444-4444-4444-4444-444444444444'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now()),
  ('55555555-5555-5555-5555-555555555555'::uuid, '66666666-6666-6666-6666-666666666666'::uuid, now())
on conflict (user_id_1, user_id_2) do nothing;
