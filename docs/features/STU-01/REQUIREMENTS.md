# Feature: STU-01 — Create endpoint GetStudentById

## Problem

Clients of the `api/v1/student` REST API can only retrieve students via `GET /api/v1/student`, which returns the entire table. Any consumer that needs a single student's profile (id, name, email, dob, computed age) must fetch and client-side-filter the full list — wasted payload and wasted client logic for a single-row lookup that the database can already do directly via primary key.

## Outcome

A client can call `GET /api/v1/student/{studentId}` and receive that student's record directly: `200 OK` with the student's JSON body when the id exists, an explicit `404 Not Found` (not a generic `500`) when it does not, and Spring's default `400 Bad Request` when the path segment is non-numeric.

## Constraints

- Runtime stays Spring Boot 3.0.6 / Java 21, unchanged; no new dependency is added to `pom.xml` (`JpaRepository.findById(Long)` already exists — see `docs/research/STU-01.md` § Pattern map).
- No schema change: `Student` entity, `StudentRepository`, and `ddl-auto=create-drop` behavior are unaffected.
- No new global error-handling infrastructure (`@ControllerAdvice` / `@ExceptionHandler`) — research risk #3 explicitly defers this to a rule-of-three trigger per `.claude/rules/reusability-baseline.md`; this story is the first endpoint needing not-found handling, not the third.
- The existing `IllegalStateException` → generic `500` pattern in `updateStudent`/`deleteStudent` is a documented ADR-0002 gap and is explicitly out of scope to retrofit in this story (surgical-changes rule: touch only what this task requires).
- No auth layer exists or is added (ADR-0002: every endpoint under `api/v1/student` is open to any caller).

## Solution sketch

Add a single-student read path that reuses the existing Service→Repository layering: the Controller exposes a new `GET` route under the same `api/v1/student` resource, the Service performs a primary-key lookup and surfaces presence/absence without throwing, and the Controller translates that into the appropriate HTTP status. No new packages, entities, or dependencies are introduced.

## Scope

- In: new `GET /api/v1/student/{studentId}` route; a new Service method that looks up a student by primary key without throwing on absence; unit test for the Service method; web-layer slice test for the Controller route (200 / 404 / 400 paths).
- Out: global `@ControllerAdvice`/`@ExceptionHandler` infrastructure; retrofitting the existing `500`-on-`IllegalStateException` pattern in `updateStudent`/`deleteStudent`; a custom error-response DTO or format beyond Spring's default JSON error body; OpenAPI/Swagger documentation; authentication/authorization; pagination (single-resource response, not a list).

## Functional requirements

FRs trace 1:1 to story ACs; see `docs/stories/STU-01.md` for canonical wording.
New impl constraints introduced below:

**STU-01-FR-1** — Response body reuses the existing `Student` entity shape  *(extends AC #1 with: no new DTO)*

The `200` response body on the found path is the `Student` entity returned as-is (same serialization the existing `GET /api/v1/student` list endpoint already produces per row: `id`, `name`, `email`, `dob`, and the `@Transient`-computed `age`). Do not introduce a separate response DTO for this endpoint.

**STU-01-FR-2** — Not-found path uses `Optional`, not a thrown exception  *(extends AC #2 with: Service/Controller responsibility split)*

The new Service method returns `Optional<Student>` (wrapping `studentRepository.findById(studentId)` directly, no new Repository method). It does not throw `IllegalStateException` for the absent case — that is the anti-pattern AC #2 explicitly calls out. The Controller checks the `Optional`: present → `ResponseEntity.ok(student)`; empty → `ResponseEntity.notFound().build()`, which yields a real `404` with no stack trace or internal exception detail in the response body.

**STU-01-FR-3** — Non-numeric path variable requires no custom validation code  *(extends AC #3 with: explicit non-scope statement)*

Spring's default path-variable-to-`Long` conversion already returns `400 Bad Request` when `{studentId}` is non-numeric. No custom `@ExceptionHandler`, regex constraint, or manual parsing is added for this case — this FR exists only to document that the `400` is framework-default behavior, not a Service/Controller code path to build.

## Non-functional requirements

- Performance: single `findById` primary-key lookup, already indexed via `JpaRepository`; no new query is added. Per `.claude/rules/performance-baseline.md`: the pagination requirement does not apply — this endpoint returns exactly zero or one row, never a list. No p95 latency budget is set (story NFR, resolved N/A — tutorial-scope project, no SLA precedent, no metrics tooling per ADR-0002 to measure one). I/O timeout behavior is inherited unchanged from the existing `StudentRepository`/HikariCP configuration; this story adds no new I/O call shape.
- Security: Per `.claude/rules/security-baseline.md`: applies to this new endpoint. The `404` response body must contain no stack trace or internal exception identifier (satisfied by `ResponseEntity.notFound().build()`, not an uncaught exception). No PII (`name`, `email`) is written to any new log statement — this story adds no new logging. No auth/ownership check applies (ADR-0002: `api/v1/student` has no auth boundary; this is unchanged by this story).
- Accessibility: N/A — JSON REST endpoint, not a UI surface. `.claude/rules/accessibility-baseline.md` does not apply to this feature.
- Observability: No new logging or metrics infrastructure is introduced (ADR-0002: no Actuator/Micrometer in this repo). The not-found path relies on Spring Boot's existing default console logging only.

## Visual spec

Not applicable — integrations.design = none. Backend / API / data feature.

## Rollout plan

- **Strategy**: bang-bang — additive `GET` route on an existing resource, fully backwards compatible with the existing list/create/update/delete endpoints; no client migration required.
- **Feature flag**: none — trivial to revert (delete the new Controller method and Service method; no data or contract changes to unwind).
- **Backout plan**: revert the commit adding the new `@GetMapping("/{studentId}")` handler and the new Service method; no schema, config, or dependency changes to roll back.
- **Success signal**: `StudentControllerTest` and `StudentServiceTest` pass in CI (`mvn test`) covering the `200`/`404`/`400` paths; no other endpoint's behavior changes (existing tests, once added for this repo's first slice tests, continue to pass).

## Documentation requirements

- **README updates**: `README.md` — this tutorial's README documents every CRUD step in sequence (see "Implement CRUD using Database"); add a short subsection after the existing Read/list section showing the new `GET /api/v1/student/{studentId}` route, and a sample request/response pair for both the found (`200`) and not-found (`404`) cases, matching the existing `.http`-snippet style already used for the Create/Update/Delete sections. Not required by the "new runnable surface" trigger (no new server/app/CLI is introduced), but included for parity since README is this repo's only documentation surface and already documents every other endpoint.
- **Runbook**: none — no runbook exists in this tutorial-scope repo (ADR-0002: no operability tooling to document).
- **API reference**: none — no OpenAPI/Swagger spec exists in this repo (ADR-0002); this story does not introduce one.
- **Inline code comments**: none — existing `StudentController`/`StudentService` methods carry no method-level comments; the new methods match that existing style.
- **Examples / how-to**: covered by the README subsection above; no separate `docs/<feature>.md` file for a tutorial repo whose sole doc surface is `README.md`.

## Open questions

None — research verdict is GO with 0 open clarifications, and the story's Decision log already resolved the only ambiguity raised during validation (the p95 latency NFR, resolved N/A).

Decisions logged in `docs/stories/STU-01.md` § Decision log.

## Approvals

- **PO approval**: Jai Karvir (jai.karvir@apexon.com) — 2026-08-12 — **APPROVE**
- Design review: N/A for this backend-only feature (`integrations.design = none`).
