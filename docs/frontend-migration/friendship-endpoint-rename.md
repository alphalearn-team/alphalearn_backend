# Frontend Migration: Friendship Endpoint Rename

## Goal

Move all friendship API usage from the legacy non-`/api/me` roots to the canonical current-user roots.

## Replace These Routes

- `GET /api/friends` -> `GET /api/me/friends`
- `DELETE /api/friends/{friendPublicId}` -> `DELETE /api/me/friends/{friendPublicId}`
- `POST /api/friend-requests` -> `POST /api/me/friend-requests`
- `GET /api/friend-requests?direction=INCOMING|OUTGOING` -> `GET /api/me/friend-requests?direction=INCOMING|OUTGOING`
- `PATCH /api/friend-requests/{requestId}` -> `PATCH /api/me/friend-requests/{requestId}`
- `DELETE /api/friend-requests/{requestId}` -> `DELETE /api/me/friend-requests/{requestId}`

## Frontend Work Items

1. Update friendship endpoint constants in the API client layer.
2. Update any React hooks, service modules, or fetch wrappers that call friendship endpoints directly.
3. Update mocked handlers, integration tests, and contract tests that hardcode the legacy paths.
4. Verify these user flows end to end:
   - Load friends list
   - Send friend request
   - View incoming requests
   - View outgoing requests
   - Approve request
   - Reject request
   - Cancel outgoing request
   - Remove existing friend

## Rollout Notes

- Backend currently supports both the new canonical routes and the legacy routes.
- Frontend should migrate to the canonical routes immediately.
- Do not add new frontend code against the legacy routes.
- Once frontend rollout is complete, backend can remove the legacy aliases.
