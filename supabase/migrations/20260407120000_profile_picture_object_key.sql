alter table if exists public.learners
    add column if not exists profile_picture_object_key text;
