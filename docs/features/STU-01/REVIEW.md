# Code Review — feature/STU-01 (HEAD `1cb84da`)

- Date: 2026-08-12T23:10:00Z
- Mode: story (`STU-01`), target `HEAD` on `feature/STU-01` vs `main` (single commit `1cb84da`)
- Files reviewed: 24 (`git diff main...HEAD --name-only`)
- Verdict: PASS

## Executive summary

STU-01 adds a single `GET /api/v1/student/{studentId}` read path: `StudentService.getStudentById(Long) -> Optional<Student>` (a pure pass-through of `studentRepository.findById`), a `StudentController` handler that maps the `Optional` to `200`/`404` via `ResponseEntity` exactly as ADR-1 in `PLAN.md` specifies, a Mockito unit test, a `@WebMvcTest` web-slice test covering all 5 declared TCs, and a matching README subsection. Layering (`Controller → Service → Repository`), constructor injection, package-by-feature layout, and the entity-as-response-model convention are all preserved. No new dependency, schema change, or auth surface is introduced; the diff correctly does not propagate the `IllegalStateException`→`500` anti-pattern from `deleteStudent`/`updateStudent` into the new read path (ADR-1, REQUIREMENTS.md § Constraints, ADR-0002 honored). SAST-shaped grep of the diff (no `eval`, no string-concatenated SQL, no logged PII, no hardcoded secrets) returned no hits.

One process-integrity issue was found and is new relative to the cached implementation-step review: the same commit that ships the feature also restores `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, and `mvnw.cmd`, even though `state.json`'s own `AF-01` flag record explicitly disposed of that work as "Deferred — not fixed in this session... belongs in its own follow-up," and `PLAN.md`'s declared file table (F-01..F-05) never lists these three files. This is flagged as a MEDIUM scope-creep finding below; it does not block merge (build-tooling only, no behavior change) but should be reconciled.

Re-confirmed against the current diff: no regression versus the cached PASS — the six review dimensions, ADR-1, and all declared file-scope boundaries for the application code (F-01..F-05) still hold. `docs/features/STU-01/state.json` shows validation was independently re-run (`/arh-validate-feature STU-01`, 5/5 TCs PASS, 8/8 unit-test regression green) since the cached review, and nothing in that re-run touched the application/test/README files reviewed here.

Note: the working tree also carries **uncommitted** changes to `pom.xml` (`java.version` 17→21) and `src/main/resources/application.properties` (local datasource username), plus untracked build artefacts (`target/**`) and `docs/features/STU-01/VALIDATION-20260812-2219.md`. These are outside `git diff main...HEAD` (not part of commit `1cb84da`) and are excluded from this review's scope, consistent with the prior review's treatment of the same local-environment drift.

🟢 strengths: ADR-1 followed exactly; layering/idioms preserved; tests cover all 5 declared TCs; no SAST hits; no scope-creep in the application code file set (F-01..F-05).
⚠️ warnings: build-tooling restoration (`mvnw`/`mvnw.cmd`/`.mvn/wrapper/maven-wrapper.properties`) bundled into the feature commit despite a recorded "defer" decision on AF-01 — see F-1.
🛑 blockers: none.

## File categorisation

| Category | Files | Count |
|---|---|---|
| Routes/API | `StudentController.java` | 1 |
| Services | `StudentService.java` | 1 |
| Tests | `StudentServiceTest.java`, `StudentControllerTest.java` | 2 |
| Docs (product) | `README.md` | 1 |
| Build tooling (Config) | `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd` | 3 |
| Harness paper trail (excluded from architecture/pattern scoring) | `docs/features/STU-01/{FLAGS,PLAN,REQUIREMENTS,REVIEW,VALIDATION-20260812-2220,state.json,evidence/*.log×4}`, `docs/research/STU-01.md`, `docs/stories/STU-01.md`, `docs/test-cases/STU-01.json`, `docs/requirements/RTM.md`, `docs/state/features.json` | 16 |
| **Total** | | **24** |

## Findings summary

| Severity | Count | Category distribution |
|----------|-------|------------------------|
| CRITICAL |   0   | — |
| HIGH     |   0   | — |
| MEDIUM   |   1   | scope-creep (1) |
| LOW      |   0   | — |

ADR violations: 0. Scope-creep: 1.

## Detailed findings

### MEDIUM

#### F-1 — scope-creep: Maven wrapper restoration bundled into the feature commit despite a recorded "defer" decision
- Category: scope-creep
- Path: `.mvn/wrapper/maven-wrapper.properties`, `mvnw`, `mvnw.cmd` (all changed in commit `1cb84da`)
- Source: `docs/features/STU-01/PLAN.md` § 2 File and Module Plan (declared file table F-01..F-05 does not include these three files) + `docs/features/STU-01/state.json` → `agent_flags[AF-01]` (`status: "defer"`, `decision: "Deferred — not fixed in this session."`, `rationale: ".mvn/wrapper/ was never committed to this repo; restoring it is unrelated to the GetStudentById endpoint and belongs in its own follow-up."`) + `.claude/rules/surgical-changes.md` ("Touch only what the current task requires"; "If a task genuinely needs a refactor of adjacent code to complete, escalate to the user before doing it; do NOT bundle silently.")
- Description: Commit `1cb84da` — the same commit implementing `getStudentById` — also restores the Maven wrapper files. `state.json`'s own `AF-01` flag record explicitly disposes of that exact work as deferred and out of scope for this story ("belongs in its own follow-up"), and `PLAN.md`'s file table never lists `mvnw`/`mvnw.cmd`/`.mvn/wrapper/maven-wrapper.properties`. The commit message ("Refs: STU-01") gives no indication build tooling was touched. The paper trail is additionally internally inconsistent: `FLAGS.md` still shows `AF-01` as `status=defer`, while `state.json`'s `pending_carry_forward[AF-01-carry]` entry carries a `resolved_at` of `2026-08-12T22:00:35Z` — a timestamp earlier than the `22:10:00Z` "Deferred" decision recorded in the very same file, i.e., the record claims the flag was both resolved and (ten minutes later) re-deferred.
- Suggested fix: Split the wrapper restoration into its own commit (or a prior one) referencing `AF-01-carry` directly, and reconcile `FLAGS.md`/`state.json` so `AF-01`'s recorded disposition (`defer` vs `resolved`) matches what actually shipped and in what order. If keeping it bundled is intentional, say so explicitly in the commit message and update the flag status to `resolved` with a corrected timeline.

## What went well

- `StudentService.getStudentById` (`src/main/java/com/example/demo/student/StudentService.java:28-30`) is a pure pass-through of `studentRepository.findById(studentId)` — matches ADR-1 and the `spring-boot-patterns` Repository-lookup idiom (`Optional<Entity>` resolved by the caller, never `.get()`).
- `StudentController.getStudentById` (`src/main/java/com/example/demo/student/StudentController.java:25-30`) keeps HTTP-status translation in the Controller and data access in the Service, matches the existing `@RequestMapping`/`@GetMapping(path = "{studentId}")` style used by `deleteStudent`/`updateStudent`, and correctly avoids the `IllegalStateException`→`500` anti-pattern for the new read path.
- `StudentServiceTest.java` and `StudentControllerTest.java` follow AAA structure, mock at the correct boundary (`StudentRepository` for the unit test, `StudentService` via `@MockBean` for the web-slice test), and cover all 5 declared test cases (`STU-01-TC-01..05`), including the security-relevant "no stack trace / no `Exception` substring in the 404 body" assertion required by `.claude/rules/security-baseline.md`.
- `docs/test-cases/STU-01.json` shows all 5 TCs re-verified PASS against a live server in the most recent `/arh-validate-feature` run, and `coverage_audit.uncovered` is empty.
- README subsection matches the existing `.http`-snippet request/response style used by the Create/Update/Delete sections, per REQUIREMENTS.md § Documentation requirements.

## Recommendation

PASS. No CRITICAL or HIGH findings, and the single MEDIUM finding (F-1, scope-creep on the bundled Maven wrapper restoration) is a build-tooling/process-integrity issue, not an application defect — it does not gate merge but should be reconciled in `FLAGS.md`/`state.json` and ideally split into its own commit before or shortly after merge. Proceed to `/arh-security-review` or address F-1 first.
