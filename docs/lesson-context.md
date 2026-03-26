# Lesson Module Context

## Scope
This document summarizes the lesson domain implemented under `src/main/java/com/example/demo/lesson` and the directly connected admin moderation code under `src/main/java/com/example/demo/admin/lesson`. It is written as a handoff artifact for another AI or engineer who needs working context before making changes.

## High-Level Purpose
The lesson module lets contributors author lessons, associate them with concepts, optionally structure them into typed sections, submit them for moderation, and expose approved lessons publicly. The same core lesson entity powers three different views:
- Public browsing of approved lessons.
- Contributor authoring and owner-only detail views.
- Admin moderation and review workflows.

## Package Map
- `lesson`: core entity model, public/contributor controller, orchestration services, mapping helpers, repositories, moderation status enums.
- `lesson.dto`: request and response payloads for public and contributor endpoints.
- `lesson.query`: dynamic list filtering by audience, concept, contributor, and moderation status.
- `lesson.moderation`: auto-moderation abstraction, local keyword implementation, moderation history model.
- `admin.lesson`: admin list/review/approve/reject endpoints built on top of the same lesson services.

## Core Data Model

### `Lesson`
Table: `lessons`

Key fields:
- `lessonId`: internal integer PK.
- `publicId`: external UUID, generated on persist.
- `title`: required string.
- `content`: JSONB legacy lesson body. May be `null` for purely section-based lessons.
- `lessonModerationStatus`: enum stored as DB enum `lessons_moderation_status`.
- `contributor`: required owning contributor.
- `concepts`: many-to-many link via `lesson_concepts`.
- `createdAt`: creation timestamp.
- `deletedAt`: soft-delete marker.
- `sections`: one-to-many `LessonSection` children with cascade + orphan removal.

Important behavior:
- Public lookup always uses `publicId` rather than internal `lessonId`.
- Soft-deleted lessons are treated as not found by listing/public lookup logic.
- A lesson can carry both legacy `content` and structured `sections` at once.

### `LessonSection`
Table: `lesson_sections`

Key fields:
- `sectionId`: internal integer PK.
- `publicId`: external UUID, generated on persist unless preserved during update.
- `lesson`: owning lesson.
- `orderIndex`: zero-based section ordering.
- `sectionType`: enum stored as DB enum `section_type`.
- `title`: optional section title.
- `content`: JSONB payload whose shape depends on `sectionType`.
- `createdAt`, `updatedAt`.

Supported section types:
- `TEXT`
- `EXAMPLE`
- `CALLOUT`
- `DEFINITION`
- `COMPARISON`

## Related Domain Dependencies
- `Contributor` is a promoted `Learner` sharing the same UUID primary key (`contributor_id`).
- `SupabaseAuthUser.userId()` is used as the authoritative authenticated user id.
- `SupabaseAuthUser.isContributor()` checks `contributor != null && contributor.isCurrentContributor()`.
- Lesson author data exposed in APIs comes from `contributor.learner.publicId` and `contributor.learner.username`.
- Lessons are tagged with `Concept` records by concept public UUID at the API layer.

## Main Services

### `LessonService`
This is the primary application service for contributor and public flows.

Responsibilities:
- List authored lessons for the current contributor.
- List public lessons.
- Return lesson detail with owner/public view switching.
- Create lessons.
- Update lessons.
- Submit lessons for review.
- Unpublish lessons.
- Soft-delete lessons.
- Map entities into contributor/public DTOs.

Notable rules:
- `createLesson` requires `title` and at least one `conceptPublicId`.
- Creation requires at least one of `content` or `sections`.
- Update requires `title` and at least one of non-empty `content` or `sections`.
- Contributor ownership is enforced by comparing `lesson.contributor.contributorId` to `user.userId()`.
- Updating a lesson that was `APPROVED` or `PENDING` resubmits it for review instead of keeping its old moderation state.
- Updating a lesson in `UNPUBLISHED` or `REJECTED` keeps it in that state unless the caller explicitly submits later.
- `getLessonDetailForUser` returns contributor detail only for the owner and only when the lesson is not soft-deleted; everyone else gets the public-approved view.

### `LessonSectionService`
Encapsulates all section CRUD behavior and section payload validation.

Responsibilities:
- Create initial sections for a new lesson.
- Replace all sections during update.
- Fetch lesson sections ordered by `orderIndex`.
- Map section entities to DTOs.
- Validate section type-specific JSON structure.

Validation rules by type:
- `text`: requires non-empty `html` string.
- `example`: requires non-empty `examples` array; each element needs non-empty `text`.
- `callout`: requires `variant` in `info|warning|tip|note` and non-empty `html`.
- `definition`: requires non-empty `term` and `definition` strings.
- `comparison`: requires non-empty `items` array; each item needs non-empty `label` and `description`.

Important implementation detail:
- `replaceSectionsForLesson` validates any supplied `sectionPublicId` values first, then deletes all existing rows, flushes, and recreates the full section list. Existing sections keep their public UUIDs only if the request resends them.
- This means section updates are replace-all semantics, not patch semantics.
- Recreated sections receive fresh `createdAt`/`updatedAt` timestamps even when their `sectionPublicId` is preserved.

### `LessonLookupService`
A thin lookup wrapper around `LessonRepository`.

Methods:
- `findByPublicIdOrThrow`: any lesson regardless of moderation state.
- `findPublicByPublicIdOrThrow`: only `APPROVED` and non-deleted lessons.

### `LessonMappingSupport`
Provides reusable mapping helpers for:
- author summary
- concept ids / concept public ids
- concept summary DTOs

### `LessonModerationWorkflowService`
Owns moderation state transitions and moderation history persistence.

Responsibilities:
- Submit lesson for review through auto-moderation.
- Fallback to manual review if auto-moderation throws.
- Unpublish lesson.
- Admin approve lesson.
- Admin reject lesson.
- Persist `LessonModerationRecord` entries for auto/admin actions.

Critical moderation behavior:
- Auto-moderation `APPROVE` does not publish the lesson. It maps to `PENDING` for manual admin review.
- Auto-moderation `FLAG` also maps to `PENDING`.
- Auto-moderation `REJECT` maps to `REJECTED` immediately.
- Exceptions during moderation map to `PENDING` with an `AUTO_FAILED` history record.
- Admin approve/reject only work from `PENDING`.

## Moderation Subsystem

### Status Lifecycle
Enum: `LessonModerationStatus`
- `UNPUBLISHED`
- `PENDING`
- `APPROVED`
- `REJECTED`

Typical flows:
1. Contributor creates lesson without submit -> `UNPUBLISHED`.
2. Contributor creates lesson with submit -> auto-moderation runs.
3. Auto moderation result:
   - `APPROVE` -> lesson becomes `PENDING`.
   - `FLAG` -> lesson becomes `PENDING`.
   - `REJECT` -> lesson becomes `REJECTED`.
   - exception -> lesson becomes `PENDING`.
4. Admin manually approves `PENDING` -> `APPROVED`.
5. Admin manually rejects `PENDING` -> `REJECTED`.
6. Owner can unpublish -> `UNPUBLISHED`.
7. Owner can resubmit only from `UNPUBLISHED` or `REJECTED`.

### Auto Moderation Implementation
Current implementation: `KeywordLessonAutoModerationService`

Provider name:
- `LOCAL_KEYWORD_RULES`

Keyword rules:
- Reject keywords: `hate`, `violence`, `kill`
- Flag keywords: `cheat`, `unsafe`, `nsfw`

Important limitation:
- It only scans `lesson.title` and legacy `lesson.content` JSON stringified text.
- It does not inspect `lesson.sections`.
- A section-only lesson with no legacy `content` may bypass keyword detection entirely unless the title triggers moderation.

### Moderation History
Entity: `LessonModerationRecord`
Table: `lesson_moderation_records`

Stored fields include:
- event type
- decision source
- resulting status
- recorded timestamp
- reasons JSON
- failure message
- raw provider response JSON
- admin review note
- actor user id
- provider name

Enums:
- `LessonModerationDecision`: `APPROVE`, `REJECT`, `FLAG`
- `LessonModerationDecisionSource`: `AUTO`, `AUTO_FALLBACK`, `ADMIN`
- `LessonModerationEventType`: `AUTO_APPROVED`, `AUTO_REJECTED`, `AUTO_FLAGGED`, `AUTO_FAILED`, `ADMIN_APPROVED`, `ADMIN_REJECTED`

How it is surfaced:
- Contributor detail returns the latest moderation reasons, latest moderation event type, latest moderation timestamp, and the latest admin rejection note if the latest admin event was a rejection.
- Admin review returns the latest automated moderation reasons and latest admin rejection note.

## Query and Filtering Layer
`LessonListQueryRepository` builds Criteria API queries based on `LessonListCriteria`.

Shared baseline filter:
- Always excludes soft-deleted lessons (`deletedAt is null`).

Audience rules:
- `PUBLIC`: only `APPROVED` lessons.
- `CONTRIBUTOR`: only lessons where `contributor.contributorId == current user`.
- `ADMIN`: all non-deleted lessons, optionally filtered by moderation status.

Concept filtering:
- Filters by joined concept internal ids.
- Uses `distinct(true)` when concept joins are present.
- Incoming concept public ids are resolved to internal ids in service layer first.

## Controllers and API Surface

### Public + Contributor Controller
Path base: `/api/lessons`

Endpoints:
- `GET /api/lessons`: list approved public lessons, optional `conceptPublicIds` filter.
- `GET /api/lessons/mine`: list current contributor's authored lessons.
- `GET /api/lessons/{lessonPublicId}`: owner gets contributor detail; others get public detail.
- `POST /api/lessons`: create lesson.
- `PUT /api/lessons/{lessonPublicId}`: update lesson.
- `POST /api/lessons/{lessonPublicId}/submit`: submit lesson for review.
- `POST /api/lessons/{lessonPublicId}/unpublish`: set to `UNPUBLISHED`.
- `DELETE /api/lessons/{lessonPublicId}`: soft delete.

Authentication expectations:
- Public list/detail can be anonymous, but detail downgrades to public view unless caller is owner.
- `/mine`, create, update, submit, unpublish, delete require authenticated contributor semantics.

### Admin Controller
Path base: `/api/admin/lessons`

Endpoints:
- `GET /api/admin/lessons`: list lessons for admin, optional `conceptPublicIds` and `status`.
- `GET /api/admin/lessons/{lessonPublicId}`: moderation review detail.
- `PUT /api/admin/lessons/{lessonPublicId}/approve`: approve pending lesson.
- `PUT /api/admin/lessons/{lessonPublicId}/reject`: reject pending lesson with required reason.

Important note:
- `AdminLessonService.requireActorUserId` checks only that `user` and `user.userId()` exist. It does not verify an admin role by itself, so admin authorization likely depends on security configuration outside this package.

## DTO Shapes

### Creation / Update Input
`CreateLessonRequest`
- `title`
- `content` (legacy JSON object)
- `conceptPublicIds`
- `submit`
- `sections`

`UpdateLessonRequest`
- `title`
- `content`
- `sections`

`CreateLessonSectionRequest`
- `sectionPublicId` for update-time identity preservation
- `sectionType`
- `title`
- `content`

### Output Models
Public summary/detail and contributor summary/detail are split intentionally:
- Public responses omit moderation internals.
- Contributor detail includes moderation history snippets.
- Detail DTOs include both legacy `content` and ordered `sections` with `totalSections`.
- Admin review detail includes moderation status, automated reasons, admin rejection reason, soft-delete timestamp, and sections.

## Repository Layer

### `LessonRepository`
Key methods:
- `findByPublicId(UUID)`
- `findPublicByPublicId(UUID)` using native SQL for approved + non-deleted filter
- count helpers used by admin dashboard
- `countLinkedLessonsByConceptId` used by admin concept deletion guard
- `unpublishByContributorId(UUID)` used when contributors are demoted
- top/low concept aggregation queries for dashboard reporting

### `LessonSectionRepository`
Key methods:
- `findByLessonIdOrderByOrderIndexAsc`
- `deleteByLesson_LessonId`
- `findByPublicId`
- `findAllByLesson_LessonId`

### `LessonModerationRecordRepository`
Key methods:
- latest overall moderation record
- latest automated moderation record
- latest admin moderation record
- full moderation history for a lesson

## Cross-Package Interactions Worth Knowing
- `admin.lesson.AdminLessonService` reuses lesson lookup, mapping, query, moderation workflow, and section services rather than duplicating logic.
- Admin moderation sends notifications through `NotificationService` to the author's learner internal id.
- `AdminContributorService` can bulk-unpublish a contributor's lessons via `LessonRepository.unpublishByContributorId(...)` when a contributor is demoted.
- `AdminConceptService` blocks concept deletion when lessons are linked, using `LessonRepository.countLinkedLessonsByConceptId(...)`.
- `lessonenrollment` references `Lesson` but is outside the core lesson authoring/moderation flow.

## Behavioral Quirks and Risks
- Dual-format content model: legacy `content` and new `sections` coexist, so callers and future code need to handle both.
- Auto moderation blind spot: section payloads are not scanned by `KeywordLessonAutoModerationService`.
- Replace-all section updates: updating sections deletes and recreates every section row.
- Section timestamps are reset on replacement, even for preserved public ids.
- Create and update treat empty map content as missing during some validations, but update still preserves backward compatibility by allowing explicit non-null content replacement once validation passes.
- Admin service does not enforce admin role locally.
- Soft-deleted lessons are still retrievable by `findByPublicIdOrThrow`, but most user-facing paths block access through higher-level checks or listing filters.
- `Lesson` has `@JsonIgnoreProperties({"profile"})` with a comment saying it is a temporary loop-prevention fix and should be revisited.

## Likely Change Hotspots
If another AI needs to extend this module, the most likely files are:
- lesson creation/update rules: `LessonService`
- section schema or validation changes: `LessonSectionService`, `SectionType`, DTOs
- moderation behavior: `LessonModerationWorkflowService`, `lesson/moderation/*`
- list filtering: `lesson/query/*`
- API response shape: `lesson/dto/*` and `admin/lesson/*`
- public/admin endpoint additions: `LessonController`, `AdminLessonController`

## Existing Tests
There is focused test coverage under `src/test/java/com/example/demo/lesson`:
- `LessonControllerTest`
- `LessonModerationWorkflowServiceTest`
- `LessonSectionServiceTest`
- `LessonSectionUpdateTest`
- `LessonServiceTest`
- `query/*` for list filtering

There is also admin-side test coverage under `src/test/java/com/example/demo/admin/lesson`.

## Suggested Mental Model
Treat the lesson module as three layers sharing one core entity:
1. Authoring layer: contributor creates and edits a draft-like lesson.
2. Moderation layer: automated screening plus manual admin review tracked in moderation history.
3. Consumption layer: public users can only see approved, non-deleted lessons, while owners and admins get richer internal views.

When changing behavior, check whether the change affects:
- legacy `content`
- section-based `content`
- contributor owner view
- public approved view
- admin moderation review
- moderation history correctness
- soft-delete visibility
