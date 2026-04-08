# API Endpoint Conventions

This project follows **resource-first, domain-first** REST-style naming.

## 1) Organize by domain, not role names in paths
- Prefer domain roots like `/api/lessons`, `/api/game-lobbies`, `/api/contributor-applications`.
- Use `/api/me/...` only for current-user scoped reads/actions.

Examples:
- `GET /api/me` (current user context)
- `GET /api/me/lessons`
- `GET /api/admin/lessons`

## 2) Use nouns for resources (avoid verb endpoints)
- Avoid: `/approve`, `/reject`, `/submit`, `/start`, `/leave` as path verbs.
- Use resource path + method + action payload when a transition is needed.

Examples:
- `PATCH /api/admin/lessons/{id}` with `{ "action": "APPROVE" }`
- `PATCH /api/lessons/{id}` with `{ "action": "SUBMIT" }`
- `PATCH /api/me/game-lobbies/private-lobbies/{id}` with `{ "action": "START" }`

## 3) Use query params for view/filter selectors
- Use query params for status/scope/view selectors instead of path suffixes like `/pending`, `/best`, `/latest`, `/current`.

Examples:
- `GET /api/admin/contributor-applications?status=PENDING`
- `GET /api/me/quizzes/{quizId}/attempts?view=BEST`
- `GET /api/admin/games/monthly-packs?scope=CURRENT`
- `GET /api/admin/dashboard?view=OVERVIEW`

## 4) Prefer plural collection names
- Use plural nouns for collection endpoints.

Examples:
- `/api/lesson-enrollments`
- `/api/me/weekly-quests`
- `/api/me/notifications`

## 5) Keep path meaning stable
- Path identifies resource.
- Query identifies selection/filter/view.
- Body identifies transition intent for mutation endpoints.

## 6) Keep backward compatibility explicit
- If an endpoint changes shape (path, query, or body), document it in release notes and frontend migration notes.
- Do not silently reuse old paths with changed semantics.
