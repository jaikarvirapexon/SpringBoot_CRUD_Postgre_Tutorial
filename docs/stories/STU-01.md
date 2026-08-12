# Story: STU-01 — Create endpoint GetStudentById

**Epic**: STU
**Status**: Validated
**Priority**: P1
**Independent test**: true
**Owner**: Unassigned
**Updated**: 2026-08-12
**Source**: intake:arh-intake

## User story

As an API consumer of the Student CRUD service (any client of `api/v1/student` — this API has no auth boundary per ADR-0002), I want to retrieve a single student's full profile by ID, so that I can display or process one student's details without fetching and filtering the entire `GET /api/v1/student` list.

## Acceptance criteria

1. Given a student with ID `X` exists in the `student` table, when a client calls `GET /api/v1/student/{studentId}` with `studentId=X`, then the API returns `200 OK` with a JSON body containing that student's `id`, `name`, `email`, `dob`, and computed `age` (matching the shape already returned by the existing list endpoint).
2. Given no student exists with the given `studentId`, when a client calls `GET /api/v1/student/{studentId}`, then the API returns `404 Not Found` with a JSON error body (e.g. `"Student with ID <id> does not exist"`) — NOT a generic `500` from an uncaught `IllegalStateException`, which is the pattern the existing `update`/`delete` methods use and which ADR-0002 flags as a known anti-pattern to avoid propagating into new work.
3. Given a non-numeric `studentId` path segment (e.g. `GET /api/v1/student/abc`), when the request is made, then Spring's default path-variable type conversion returns `400 Bad Request` (no custom validation logic required for this case — documenting existing framework behavior).

## Non-functional requirements

- Performance: single get-by-id lookup is a PK-indexed `findById` read (already provided by `JpaRepository`, no new query needed); no p95 latency budget is set for this endpoint — resolved as N/A: this is a tutorial-scope project with no SLA/latency precedent documented anywhere in the repo, and ADR-0002 confirms no metrics/observability tooling exists to measure one (see Decision log) — no pagination concern applies since this returns a single row.
- Security: none — matches ADR-0002's confirmed architecture decision that every endpoint under `api/v1/student` is open to any caller (no `spring-boot-starter-security` dependency, no auth layer exists or is planned for this tutorial-scope project).
- Accessibility: N/A — this is a JSON REST endpoint, not a UI surface; no WCAG applicability.
- Observability: no new logging/metrics infrastructure introduced (repo has no Actuator/Micrometer per ADR-0002); on the not-found path, rely on the existing default Spring Boot console logging only — do not introduce a new logging framework for this story.

## Dependencies

- Upstream: none new. `StudentRepository extends JpaRepository<Student, Long>` already exposes `findById(Long id)` — no repository or schema change is required for this story.
- Downstream: none yet — this is the first story drafted under epic STU, no sibling stories exist to depend on this one.

## Test mapping

- E2E: NA — no E2E test harness exists in this repo (test runner is JUnit 5 unit/slice tests only per `spring-boot-starter-test`; not inventing an E2E flow file that doesn't exist).
- Unit: `StudentServiceTest` (new) — get-by-id returns the student when present, surfaces a not-found signal (not a bare `IllegalStateException` swallowed as 500) when absent. `StudentControllerTest` (new, `@WebMvcTest` or MockMvc slice) — asserts `200` + body shape on found, `404` on not-found, `400` on non-numeric path variable.
- Manual: ad-hoc `.http` request smoke test (same style as the existing `generated-requests.http` usage shown in `README.md`) for local verification only — no automated manual-test tooling in this repo.

## Clarifications

(none — the p95 latency marker below was resolved via best-judgment; see Decision log)

## Decision log

- 2026-08-12 Performance NFR (p95 latency target): Resolved as N/A / no target set — no SLA or latency budget is documented anywhere in the repo, this is a tutorial-scope project with no prior NFR precedent, and ADR-0002 confirms no metrics/observability tooling exists to measure one. Resolved inline with best-judgment per requirement-validation self-correction, round 1 (no user or business context requiring a production SLA for this endpoint).

## Validation log

- 2026-08-12T00:00:00Z  v1  total=91  Clarity-Unresolved=0 (FAIL)
- 2026-08-12T00:05:00Z  v2  total=98  PASS
- 2026-08-12T00:20:00Z  re-validation (direct /arh-validate-story invocation)  total=100  Completeness=20 Testability=25 Feasibility=15 Clarity=10 Clarity-Unresolved=5 Traceability=10 NFR-Coverage=15  PASS  Status reaffirmed: Validated
