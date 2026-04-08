alter table public.friend_requests
    drop constraint if exists friend_requests_unique_pair;

drop index if exists public.friend_requests_unique_pair;

create unique index friend_requests_one_pending_per_pair
on public.friend_requests (
    least(sender_id, receiver_id),
    greatest(sender_id, receiver_id)
)
where status = 'PENDING';
