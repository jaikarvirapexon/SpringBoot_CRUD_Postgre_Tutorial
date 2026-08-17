# Research: STU-01 — Create endpoint GetStudentById

**Story**: STU-01  
**Verdict**: GO  
**Score**: 96/100  
**Upstream dependencies**: None  

---

## Exploration Log

**Objective**: Map the codebase structure for a new GET endpoint that retrieves a single student by ID.

- **Repository structure**: `src/main/java/com/example/demo/student/` contains `Student.java` (entity with @Entity, @Transient age), `StudentRepository.java` (extends JpaRepository<Student, Long>), `StudentService.java` (business logic), `StudentController.java` (REST handlers), `StudentConfig.java` (seed data).
- **StudentRepository**: Provides `findById(Long id) -> Optional<Student>` via JpaRepository; also has custom `findStudentByEmail(String email)` query method.
- **StudentService**: Three existing CRUD methods: `getStudents()` (returns list), `addNewStudent()` (throws IllegalStateException on duplicate email), `deleteStudent()` (throws IllegalStateException on not-found), `updateStudent()` (throws IllegalStateException on not-found). Pattern: Service layer throws IllegalStateException for domain violations; Controller returns void for POST/DELETE/PUT (HTTP 200 empty on success, propagates exceptions as 500).
- **StudentController**: @RestController at path "api/v1/student"; uses constructor injection of StudentService; HTTP methods: GET (list all), POST (register), DELETE /{studentId}, PUT /{studentId}. No GetMapping for single-student-by-id yet.
- **Test files**: Only `DemoApplicationTests.java` exists (context-load test, @SpringBootTest). No unit tests for Service or Controller slices yet.
- **ADR-0002 System Architecture**: Documents that error handling is a flagged gap — `IllegalStateException`s propagate as generic 500 responses; no `@ControllerAdvice` or `@ExceptionHandler` configured. This is explicitly flagged as an anti-pattern to avoid in new work (per story AC 2).
- **Framework versions**: Spring Boot 3.0.6, Java 21, Maven (mvnw), Spring Data JPA, PostgreSQL driver.

---

## Pattern map

### Existing code to extend

- **StudentService**: Add `getStudentById(Long studentId) -> Optional<Student>` method. Wraps `studentRepository.findById(studentId)` directly (no business logic, since this is a simple PK lookup).
- **StudentController**: Add `@GetMapping("/{studentId}")` method that calls the new Service method. Handles Optional: if present, return ResponseEntity.ok(Student); if absent, return ResponseEntity.notFound().build() (produces 404 with Spring's default JSON error body).

### Existing patterns to follow

- **Constructor injection**: StudentController accepts StudentService via constructor parameter with `@Autowired`; follow this pattern (match StudentController's existing style).
- **RequestMapping + HTTP method mapping**: Class-level `@RequestMapping(path = "api/v1/student")`; method-level `@GetMapping("/{studentId}")` with path variable `@PathVariable("studentId") Long studentId`.
- **Path variable binding**: Spring's default path-variable type coercion will convert {studentId} from String to Long; if the input is non-numeric (e.g., "abc"), Spring returns HTTP 400 automatically (per AC 3 — no custom validation needed).
- **Repository pattern**: Use `JpaRepository.findById(id) -> Optional<Entity>` directly; no new query method needed.
- **Package-by-feature layout**: All code stays in `com.example.demo.student/` (no new packages).

### New files to create

- **StudentServiceTest.java**: Unit test (JUnit 5 + Mockito) covering `getStudentById()` — asserts returns Optional.of(Student) when found, Optional.empty() when absent. Use `@ExtendWith(MockitoExtension.class)` and mock StudentRepository.
- **StudentControllerTest.java**: Web-layer slice test (JUnit 5 + MockMvc via `@WebMvcTest(StudentController.class)`) covering the new `@GetMapping("/{studentId}")` — asserts HTTP 200 + Student body when found, HTTP 404 when not found, HTTP 400 when path variable is non-numeric. Use MockMvc.perform(get(...)) to simulate requests.
- (Optional) Error response DTO if custom error body format is required — but story allows "e.g." example, so Spring's default error structure should suffice.

### Shared code at risk

- **StudentRepository**: Used by StudentService (all four CRUD methods) and StudentConfig (seed data via saveAll). Adding a new call to `findById()` in the Service does not change the Repository's contract — low risk of regression.
- **StudentController**: Adding a new HTTP method does not change routing for existing GET/POST/DELETE/PUT. Spring MVC routes by method type + path pattern, so no collision risk.

---

## Risk register

| # | Dimension | Severity | Description | Mitigation |
|---|-----------|----------|-------------|-----------|
| 1 | Integration | MED | Error response body for 404 Not Found — story specifies "JSON error body (e.g. `'Student with ID <id> does not exist'`)" but does not mandate exact format | Use Spring's default 404 response structure (ResponseEntity.notFound().build()) which includes `{status, error, message, timestamp, path}`; if a custom format is required during planning, capture in REQUIREMENTS.md and implement a simple error DTO or Map-based response |
| 2 | Integration | LOW | Path variable type conversion edge case (AC 3: non-numeric studentId should return 400) | Spring's default path-variable binder automatically returns 400 Bad Request if Long conversion fails; no custom code needed; document in StudentControllerTest to assert this behavior |
| 3 | Domain | LOW | Optional handling in Service vs. Controller — decision on responsibility split for null-check | Return Optional<Student> from Service (no new exception type); let Controller handle Optional.isEmpty() → 404. Avoids introducing StudentNotFoundException and keeps Service logic minimal. If future endpoints need similar patterns, then a @ControllerAdvice/@ExceptionHandler may be warranted (rule of three per `.claude/rules/reusability-baseline.md`). |

No CRITICAL risks. All risks are manageable within the story scope and have documented mitigations.

---

## Score

| Dimension | Weight | Score | Rationale |
|-----------|--------|-------|-----------|
| **Integration** | 25% | 95/100 | StudentRepository.findById() is already available; no external service calls. Deduct 5 for the error response body format ambiguity (clarified in mitigation). |
| **Compatibility** | 20% | 95/100 | New endpoint, no backward-compat risk. Student response shape matches existing list endpoint. Deduct 5 for the fact that error responses may differ slightly from existing 500-exception pattern. |
| **Domain** | 20% | 95/100 | All acceptance criteria (happy path, not-found 404, non-numeric 400) are clearly defined. Optional.isEmpty() → 404 is a standard pattern. Deduct 5 for potential edge case: what if studentId is null (though the path variable binding prevents this). |
| **Performance** | 15% | 98/100 | Single PK-indexed lookup via JpaRepository.findById(); no pagination or N+1 risk. Story explicitly marks latency budget as N/A (tutorial scope, no SLA). Deduct 2 for remote possibility of DB timeout (unlikely for a simple query). |
| **Dependency** | 20% | 100/100 | No upstream stories; JpaRepository is already a dependency. No blocking external work. |

**Total: (95×0.25 + 95×0.20 + 95×0.20 + 98×0.15 + 100×0.20) = 23.75 + 19 + 19 + 14.7 + 20 = 96.45 → 96/100**

**Verdict: GO**

---

## Synthesis

**Verdict: GO.** Score is 96/100, well above the GO threshold (≥80). The new endpoint fits seamlessly into the existing Service→Repository→Controller architecture: `StudentService.getStudentById(Long)` returns `Optional<Student>` wrapping `JpaRepository.findById()`, and `StudentController`'s new `@GetMapping("/{studentId}")` checks the Optional and returns either the Student (200) or HTTP 404 (not found) or HTTP 400 (non-numeric path variable, automatic via Spring binding). All three acceptance criteria are directly addressable without new dependencies or schema changes. The only design decision is error-response format for 404, which is clarified in the Integration risk mitigation: use Spring's default JSON structure unless planning identifies a custom format requirement. Testing is straightforward JUnit 5 + MockMvc per the existing Spring Boot Test setup. The flagged "no structured error handling" gap in ADR-0002 is explicitly addressed by avoiding the bare `IllegalStateException` anti-pattern and instead using an Optional-based pattern appropriate for a single endpoint. **Next phase: `/arh-plan-requirements STU-01`.**

---

## Clarifications

None. All acceptance criteria, dependencies, and test scope are fully specified in the story file (STU-01.md), including resolution of the performance latency question via decision log.

