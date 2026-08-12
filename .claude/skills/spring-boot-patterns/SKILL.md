---
name: spring-boot-patterns
description: spring-boot patterns for this project — fill body with team conventions. Used by implementation/validation/arh-review agents.
when_to_use: Writing or reviewing spring-boot code.
user-invocable: false
allowed-tools: Read Write Edit Bash Grep Glob
---
# spring-boot Patterns

## Idioms

- Package-by-feature, not by layer: `com.example.demo.<feature>` (e.g. `student`) holds its entity, repository, service, controller, and config together — not `controllers/`, `services/` top-level buckets.
- Class suffix names the role: `<Feature>`, `<Feature>Controller`, `<Feature>Service`, `<Feature>Repository`, `<Feature>Config`.
- Constructor injection only (`@Autowired` on the constructor of a `final` field). No field injection.
- `@RestController` + `@RequestMapping(path = "api/v1/<feature>")` at class level; HTTP method + path variable only on the method mapping.
- Entities are the request/response model for this tutorial scope — no separate DTO layer exists. Do not introduce one unless the task requires it.

## Project structure

- `src/main/java/com/example/demo/DemoApplication.java` — entry point (`@SpringBootApplication`).
- `src/main/java/com/example/demo/<feature>/` — one package per domain feature (currently `student`): `Entity.java`, `Repository.java`, `Service.java`, `Controller.java`, optional `Config.java` (seed data via `CommandLineRunner`).
- `src/main/resources/application.properties` — datasource + JPA config.
- `src/test/java/com/example/demo/` — mirrors main package structure.

## Layering & dependency rules

- Controller → Service → Repository, one direction only. Controllers never call `Repository` directly.
- Controllers hold no business logic — validation/branching lives in the Service.
- Repositories are `interface X extends JpaRepository<Entity, Id>` with `@Repository`; only query derivation or `@Query` methods, no logic.
- `Config` classes (seed data, beans) depend on `Repository`, never on `Controller`.

## Error handling

- Service layer throws `IllegalStateException` for domain violations (not-found, duplicate-email) — matches existing `StudentService` idioms; do not introduce a new exception type without updating all call sites.
- Repository lookups that may be absent return `Optional<Entity>` and are resolved in the Service via `.orElseThrow(...)`, never `.get()`.
- Don't swallow exceptions — let them propagate to Spring's default error handling rather than catching and logging silently.
- Per `.claude/rules/security-baseline.md`: user-facing error responses carry no stack traces or internal identifiers.

## Anti-patterns

- Field injection (`@Autowired` on a field) — violates constructor-injection idiom used throughout; breaks testability.
- Calling `Repository` from `Controller`, skipping `Service` — violates layering rule above.
- `findAll()` without pagination on list endpoints — violates `.claude/rules/performance-baseline.md` (pagination on every list endpoint).
- Catching `IllegalStateException` in the Controller to translate to a status code by hand on every method — duplicated logic; use a shared `@ExceptionHandler` (`@ControllerAdvice` or a `@RestController`-scoped handler) instead once more than one controller needs it (rule of three, `.claude/rules/reusability-baseline.md`).

## Examples

BAD — field injection:
```java
@RestController
public class StudentController {
    @Autowired
    private StudentService studentService;
}
```

GOOD — constructor injection:
```java
@RestController
public class StudentController {
    private final StudentService studentService;

    @Autowired
    public StudentController(StudentService studentService) {
        this.studentService = studentService;
    }
}
```

BAD — unbounded list endpoint:
```java
@GetMapping
public List<Student> getStudent() {
    return studentService.getStudents(); // no page/size bound
}
```

GOOD — paginated:
```java
@GetMapping
public Page<Student> getStudent(@RequestParam(defaultValue = "0") int page,
                                 @RequestParam(defaultValue = "20") int size) {
    return studentService.getStudents(PageRequest.of(page, size));
}
```

## References

- `README.md` — full tutorial walkthrough of the CRUD implementation these patterns are drawn from.
- `docs/adr/0001-tech-stack.md` — stack decision record (Spring Boot 3.0.6, Java 21, Maven, PostgreSQL/JPA).
- `src/main/java/com/example/demo/student/` — canonical example of the package-by-feature layout.

## API / interface contracts

- Base path: `api/v1/<feature>` — version prefix stays `v1` until a breaking contract change forces `v2`.
- Request/response body is the JPA entity directly (no DTO mapping layer in this codebase) — keep `@Transient` fields (e.g. `age`) for derived values that must not hit the DB.
- Mutating endpoints (`POST`/`PUT`/`DELETE`) currently return `void` (HTTP 200 empty body); if a task needs the created/updated resource in the response, return it explicitly rather than relying on the client re-fetching.
- Update endpoint (`PUT`) takes partial fields as `@RequestParam(required = false)`, not a partial-body PATCH — follow this convention for further partial updates rather than introducing `@PatchMapping`.

## Security (stack-specific)

- Validate `@RequestBody`/`@PathVariable`/`@RequestParam` input in the Service layer before persisting (defence-in-depth per `.claude/rules/security-baseline.md`), even though Bean Validation (`spring-boot-starter-validation`) is not currently a dependency — do not add it speculatively.
- Never log entity fields that are PII (`email`, `name`, `dob`) — log the entity `id` only.
- Repository queries use JPQL with positional/named parameters (`@Query("... WHERE s.email=?1")`) — never string-concatenate user input into a query.

## Data access & migrations

- ORM: Spring Data JPA (`JpaRepository<Entity, Long>`); prefer derived query methods, fall back to `@Query` (JPQL) only when derivation can't express the query.
- `spring.jpa.hibernate.ddl-auto=create-drop` is a **tutorial/dev-only** setting — it drops and recreates the schema on every run. Do not carry this into any environment where data must persist; a real migration tool (e.g. Flyway/Liquibase) would replace it before that point, but none is configured today — don't add one speculatively.
- Multi-step mutations that touch more than one field/row (e.g. `updateStudent`) are wrapped in `@Transactional` on the Service method — keep transaction boundaries at the Service layer, never in the Controller or Repository.

## Logging, config & observability

- Config source of truth: `src/main/resources/application.properties` — datasource URL/credentials, `spring.jpa.*` tuning, `server.error.include-message`.
- `spring.jpa.show-sql=true` / `format_sql=true` are dev-time conveniences; do not let SQL logs include bound parameter values that carry PII.
- No structured logging framework is configured beyond Spring Boot defaults — don't introduce one without a task-driven need.

## Dependency, build & CI

- Package manager: Maven via `./mvnw` (wrapper checked in — always invoke through the wrapper, not a globally installed `mvn`).
- Commands are declared in `docs/config/project-commands.yaml`: `typecheck`/`build` = `./mvnw compile` / `./mvnw clean install`; `test` = `./mvnw test`; no lint/format plugin is configured — don't invent one.
- No CI workflow files exist yet under this repo despite `CI: github-actions` in `CLAUDE.md` — if a task adds one, mirror the commands in `project-commands.yaml` rather than hand-rolling new build steps.
