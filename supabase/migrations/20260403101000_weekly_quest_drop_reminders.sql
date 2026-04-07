-- Reminder feature removed: drop no-longer-used reminder persistence objects.
drop table if exists public.weekly_quest_reminders;
drop sequence if exists public.weekly_quest_reminders_id_seq;
