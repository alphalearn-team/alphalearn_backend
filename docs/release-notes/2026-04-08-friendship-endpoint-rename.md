# Friendship Endpoint Rename

Date: 2026-04-08

## Summary

The friendship endpoints now use `/api/me/...` as the canonical current-user scope to align with [API endpoint conventions](../api-endpoint-conventions.md).

## Canonical Endpoints

- `GET /api/me/friends`
- `DELETE /api/me/friends/{friendPublicId}`
- `POST /api/me/friend-requests`
- `GET /api/me/friend-requests?direction=INCOMING|OUTGOING`
- `PATCH /api/me/friend-requests/{requestId}`
- `DELETE /api/me/friend-requests/{requestId}`

## Backward Compatibility

The previous roots remain temporarily available as compatibility aliases during frontend migration:

- `GET /api/friends`
- `DELETE /api/friends/{friendPublicId}`
- `POST /api/friend-requests`
- `GET /api/friend-requests?direction=INCOMING|OUTGOING`
- `PATCH /api/friend-requests/{requestId}`
- `DELETE /api/friend-requests/{requestId}`

## What Did Not Change

- Request bodies are unchanged.
- Query parameters are unchanged.
- Response shapes are unchanged.
- Authorization behavior is unchanged; learner-only checks still apply in the controllers.

## Removal Plan

1. Backend ships the canonical `/api/me/...` routes and keeps legacy aliases temporarily.
2. Frontend switches all friendship API calls to the canonical routes.
3. After frontend deployment is confirmed in all active environments, remove the legacy `/api/friends` and `/api/friend-requests` aliases.
