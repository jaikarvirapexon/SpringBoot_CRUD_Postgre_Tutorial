# PLAN: STU-01 — Create endpoint GetStudentById

- Status: Complete
- Story: `docs/stories/STU-01.md`
- REQUIREMENTS: `docs/features/STU-01/REQUIREMENTS.md`
- Research: `docs/research/STU-01.md` (Verdict: GO, 96/100)

## 1. Architecture Decisions

### ADR-1: Service returns `Optional<Student>` for the not-found case, not a thrown exception · Accepted · 2026-08-12 · Jai Karvir

**Context**: Every existing mutating method on `StudentService` (`deleteStudent`, `updateStudent`) signals "not found" by throwing a bare `IllegalStateException`, which Spring's default error handling turns into an opaque `500` — an anti-pattern ADR-0002 already flags and that story AC #2 explicitly forbids propagating into this new read path. This story is the first `Service` method whose "absent" outcome must map to a specific, deliberate HTTP status (`404`) rather than an unhandled failure, so the Service/Controller responsibility split for that outcome has to be decided explicitly rather than defaulting to the existing `IllegalStateException` idiom.

**Decision**: `StudentService.getStudentById(Long studentId)` returns `Optional<Student>` — a direct pass-through of `studentRepository.findById(studentId)` with no additional Service-layer logic. `StudentController`'s new handler inspects the `Optional`: present → `ResponseEntity.ok(student)` (`200`); empty → `ResponseEntity.notFound().build()` (`404`, Spring's default JSON error body, no stack trace or internal identifier per `.claude/rules/security-baseline.md`).

**Alternatives considered**:
- Introduce a new `StudentNotFoundException` thrown by the Service and caught by a `@ControllerAdvice`/`@ExceptionHandler`: rejected — REQUIREMENTS.md Constraints explicitly excludes adding global error-handling infrastructure for a single endpoint (rule-of-three per `.claude/rules/reusability-baseline.md`; this is the first, not the third, endpoint needing not-found handling).
- Reuse the existing bare `IllegalStateException` pattern from `deleteStudent`/`updateStudent`: rejected — it propagates as an uncaught `500`, which is precisely the anti-pattern story AC #2 forbids for this endpoint.

**Consequences**:
- Positive: precise `404` semantics with zero new types and zero new shared infrastructure; the Controller, not the Service, owns the HTTP-status translation, keeping the Service a thin data-access wrapper for this read path.
- Negative: `StudentService` now carries two different "not found" idioms side by side (`Optional` for `getStudentById`, thrown `IllegalStateException` for `deleteStudent`/`updateStudent`) until a third endpoint triggers the rule-of-three `@ControllerAdvice` refactor called out in research risk #3. Reversible mechanically — swapping `getStudentById` to a custom exception later is a single-method, single-caller change with no data or schema impact.

State record: `docs/features/STU-01/state.json` → `.decisions[0]` (`D-01`), `adr_ref: null` (mini-ADR only; scope is local to this endpoint, not promoted to `docs/adr/` since it does not affect a second story).

## 2. File and Module Plan

| ID   | Action | Path                                                                   | Reason                                                                                          |
|------|--------|-------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------|
| F-01 | modify | `src/main/java/com/example/demo/student/StudentService.java`           | Add `getStudentById(Long studentId) -> Optional<Student>` per ADR-1                              |
| F-02 | modify | `src/main/java/com/example/demo/student/StudentController.java`        | Add `@GetMapping("/{studentId}")` handler consuming F-01 — entry-registration site for the new Service method |
| F-03 | create | `src/test/java/com/example/demo/student/StudentServiceTest.java`       | Unit test (JUnit 5 + Mockito) for `getStudentById` — found and absent cases                      |
| F-04 | create | `src/test/java/com/example/demo/student/StudentControllerTest.java`    | `@WebMvcTest(StudentController.class)` slice test for `GET /{studentId}` — `200`/`404`/`400` paths |
| F-05 | modify | `README.md`                                                             | Add GET-by-id subsection with `.http` request/response examples (`200` and `404`) per Documentation requirements |

No new files, packages, or dependencies. No new Repository method — `JpaRepository.findById(Long)` (F-01's collaborator) already exists and is unchanged.

## 3. Module Hierarchy

```
student/
├── StudentService (modified)
│   - input:  Long studentId
│   - output: Optional<Student>
│   - public: getStudentById(Long studentId) -> Optional<Student>
└── StudentController (modified)
    - input:  HTTP GET api/v1/student/{studentId}  (path variable coerced to Long by Spring's binder)
    - output: ResponseEntity<Student>  (200 + body when present | 404 empty when absent | 400 when {studentId} is non-numeric, framework default — no code path)
    - public: getStudentById(Long studentId) -> ResponseEntity<Student>
```

## 4. State and Data Management

- No new persistent state: no new entity fields, no new table/column, no migration. `Student`, `StudentRepository`, and `spring.jpa.hibernate.ddl-auto=create-drop` behavior are unchanged (REQUIREMENTS.md Constraints).
- No cache introduced: single PK-indexed `findById` read per request, no TTL/invalidation concern.
- No client-side state: this is a backend JSON API change; no store/context boundaries apply.

## 5. Task Breakdown

| #    | Title                                                          | Complexity | [P] | Predecessors | Files       | Notes                                                                                                                                     |
|------|-----------------------------------------------------------------|------------|-----|--------------|-------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| T-01 | Add `StudentService.getStudentById` + unit test                | S          |     | —            | F-01, F-03  | Pure PK-lookup pass-through, no business logic (ADR-1 resolves research risk R-3 — Optional-vs-exception). AAA test structure per `junit-patterns`; mock `StudentRepository`. |
| T-02 | Add `StudentController` GET-by-id handler + web-slice test      | S          |     | T-01         | F-02, F-04  | `ResponseEntity.notFound().build()` on absent addresses research risk R-1 (404 body format — use Spring default, no custom DTO). Non-numeric path variable → `400` is framework-default (research risk R-2); test asserts it, no new code. `@WebMvcTest` mocks the Service. |
| T-03 | Document the new endpoint in README                            | S          |     | T-02         | F-05        | New subsection placed after the existing "Read"/list section; `.http`-style request/response pairs for `200` (found) and `404` (not found), matching the existing Create/Update/Delete section style. |

## 6. Carry-Forward Risks and Conditions

Risks from `docs/research/STU-01.md` § Risk register. No HIGH/CRITICAL risks exist for this story; all three MED/LOW risks are resolved by ADR-1 and the tasks below (re-stated here for traceability per plan-authoring, not because verification requires it).

### Risks addressed by tasks

| Risk id | Severity | Addressed by |
|---------|----------|---------------|
| R-1     | MED      | T-02          |
| R-2     | LOW      | T-02          |
| R-3     | LOW      | T-01 (ADR-1)  |

### Risks accepted (carry-forward)

None — all three research risks are addressed by tasks above; nothing deferred to `pending_carry_forward[]`.

### Cross-Feature Dependency Notes

None. STU-01 is the first story under epic STU (`docs/stories/STU-01.md` § Dependencies: "no sibling stories exist to depend on this one"); no other in-flight feature's artefacts are consumed here.

## 7. Test Strategy

| Layer      | Test path                                                             | TCs covered  | Notes                                                                                   |
|------------|------------------------------------------------------------------------|--------------|-------------------------------------------------------------------------------------------|
| Unit       | `src/test/java/com/example/demo/student/StudentServiceTest.java`      | STU-01-TC-01, TC-02 | Mockito-mocked `StudentRepository`; asserts `Optional.of(Student)` when found, `Optional.empty()` when absent. No Spring context. Produced by T-01. |
| Web slice  | `src/test/java/com/example/demo/student/StudentControllerTest.java`   | STU-01-TC-01, TC-02, TC-03, TC-04, TC-05 | `@WebMvcTest(StudentController.class)` + `MockMvc`, `StudentService` mocked via `@MockBean`; asserts `200`+body (TC-01), `404` (TC-02, TC-04), `400` non-numeric path variable (TC-03), and no-stack-trace-in-body (TC-05). No real database. Produced by T-02. |
| Manual     | `README.md` GET-by-id subsection (`.http`-style snippets)             | STU-01-TC-01, TC-02 | Ad-hoc local smoke test only, no automated manual-test tooling in this repo (per story Test mapping). Produced by T-03; not a gating check. |

`docs/test-cases/STU-01.json` exists (written during `/arh-plan-requirements` Phase 2): 5 TCs (`STU-01-TC-01..05`), `coverage_audit.uncovered: []` — every FR (FR-1..3) and the security NFR has ≥1 TC. TC-04 (negative/zero id → 404) and TC-05 (no stack trace in 404 body) are both exercised by `StudentControllerTest` alongside TC-01/02/03; no separate task is needed since they're additional assertions within T-02's web-slice test, not new files.

### Coverage gates

- Unit + web-slice tests run via the existing `./mvnw test` command (`docs/config/project-commands.yaml` → `test:` / `test_unit:`); no separate `test_integration`/`test_e2e` runner exists or is introduced (per `junit-patterns` — this project runs one `src/test/java` tree).
- No E2E, performance, or contract TCs are declared for this story (per `docs/stories/STU-01.md` § Test mapping: "E2E: NA — no E2E test harness exists in this repo") — no test-runner setup task is required.
- No new runtime dependency, service, or port is introduced — no `docs/config/project-commands.yaml preflight:` or `docs/config/stack-smoke.md` update is required.

### Plan validation rounds

| Round | Verdict | Failing dimensions | Action                     |
|-------|---------|---------------------|-----------------------------|
| 1     | PASS    | —                   | Proceed to `/arh-implement` |

## Plan validation

- Date: 2026-08-12T14:00:00Z
- Verdict: PASS
- Wiring:          PASS  (No new module files are created — only two existing classes (`StudentService`, `StudentController`) are modified, and F-02 already lists `StudentController` as the consumer/entry-registration site for F-01's new `getStudentById` method. F-03/F-04/F-05 are leaf files — test files and README — which are exempt from the wiring check.)
- Docs:            PASS  (T2 fires: new HTTP route `GET /api/v1/student/{studentId}`. T-03 adds a task updating root `README.md` with route, method, and request/response examples. T1/T3/T4 do not fire — no new runnable surface, env var, or service/port.)
- Runner-setup:    PASS  (No TC in the test-strategy table is `type: e2e | performance | contract` — only Unit and Web-slice, both run by the already-configured `./mvnw test`. No trigger fires.)
- Cross-section:   PASS  (Every file table row (F-01..F-05) is referenced by exactly one task's Files column (T-01: F-01,F-03; T-02: F-02,F-04; T-03: F-05). Every test-strategy layer (Unit, Web slice, Manual) maps to a producing task (T-01, T-02, T-03 respectively). No task references a file outside the file table.)
- Config drift:    PASS  (No new runtime dependency, service directory, `docker-compose.yml` entry, or port is introduced — C1/C2/C3 do not fire. `docs/config/project-commands.yaml` and `docs/config/stack-smoke.md` remain accurate as-is.)
- Rounds:          1
