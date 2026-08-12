---
name: junit-patterns
description: junit test patterns for this project — fill body with team test conventions. Used by validation/arh-implementation agents.
when_to_use: Writing or reviewing junit tests.
user-invocable: false
allowed-tools: Read Write Edit Bash Grep Glob
---
# junit Test Patterns

<!-- Harness scaffold: test-runner=junit — STRUCTURE only; -->
<!-- Fill every CORE section. Under OPTIONAL, keep only what applies and DELETE the rest -->
<!-- (heading+slot) before filling. OPTIONAL slots are not lint-nagged; CORE TODO slots are. -->
<!-- Loaded by validation-agent (and implementation-agent for test code) when this runner is active. -->

## Idioms

- Test files live in `src/test/java`, mirroring the package of the class under test (e.g. `com.example.demo.student.StudentServiceTest` for `com.example.demo.student.StudentService`).
- Name test classes `<ClassUnderTest>Test`; name test methods `methodName_condition_expectedResult` or a descriptive `should...` sentence — pick one style per class, don't mix.
- Structure every test body as Arrange-Act-Assert (AAA) with a blank line between each block; no interleaving.
- Build test fixtures inline with plain constructors (e.g. `new Student("Alex", "alex@gmail.com", LocalDate.of(...))`) — this project has no test-data-builder/factory library; don't introduce one for a single test class.
- Use JUnit 5 (`spring-boot-starter-test`) — `@Test`, `@BeforeEach`, `@ExtendWith(MockitoExtension.class)` for pure Mockito unit tests, `@DataJpaTest`/`@SpringBootTest` for Spring-context tests.
- Assert with AssertJ (`assertThat(...)`), already pulled in transitively by `spring-boot-starter-test`; avoid mixing in JUnit's own `assertEquals` in the same class.

## Test layering

- **Unit** (`StudentServiceTest`, plain Mockito): mock `StudentRepository`, test business rules in `StudentService` (email-exists checks, update logic) in isolation. No Spring context, no DB.
- **Repository/slice** (`@DataJpaTest`): test `StudentRepository` custom queries (e.g. `findStudentByEmail`) against an embedded/real datasource; must NOT reach into controller or service layers.
- **Web/controller** (`@WebMvcTest` + `MockMvc`): test `StudentController` request mapping, path/query param binding, and status codes with the service layer mocked. Must NOT hit a real database.
- No `test_integration`/`test_e2e` runner is configured in `docs/config/project-commands.yaml` for this project — only `./mvnw test` runs the single `src/test/java` tree. Do not invent a separate integration source set; keep all tests runnable by `./mvnw test`.

## Mocking & test data

- Mock collaborators one layer down only: service tests mock the repository; controller tests mock the service. Don't mock the class under test itself.
- Use Mockito (`@Mock`, `@InjectMocks`, or `@MockBean` in a `@WebMvcTest`) — already available via `spring-boot-starter-test`.
- Seed data as literal `Student` instances per test (or a small `@BeforeEach` shared fixture within one class) — no shared cross-class fixture files exist or are needed at this project's size.
- `@DataJpaTest` repository tests must run against a real/embedded relational datasource, not a mocked `EntityManager` — mocking JPA query derivation defeats the point of the slice test.

## Examples

BAD (no AAA separation, asserts on mocked-through behavior):
```java
@Test
void addNewStudent() {
    when(studentRepository.findStudentByEmail(any())).thenReturn(Optional.empty());
    studentService.addNewStudent(new Student("Alex", "a@x.com", LocalDate.now()));
    verify(studentRepository).save(any());
}
```

GOOD (AAA, asserts the actual saved value):
```java
@Test
void addNewStudent_emailNotTaken_savesStudent() {
    // Arrange
    Student student = new Student("Alex", "a@x.com", LocalDate.now());
    given(studentRepository.findStudentByEmail(student.getEmail())).willReturn(Optional.empty());

    // Act
    studentService.addNewStudent(student);

    // Assert
    ArgumentCaptor<Student> captor = ArgumentCaptor.forClass(Student.class);
    verify(studentRepository).save(captor.capture());
    assertThat(captor.getValue().getEmail()).isEqualTo("a@x.com");
}
```

BAD (asserts thrown exception message via try/catch, no clear failure case):
```java
@Test
void deleteStudent() {
    try {
        studentService.deleteStudent(99L);
    } catch (Exception e) {
        assertThat(e.getMessage()).contains("does not exist");
    }
}
```

GOOD (uses AssertJ's exception assertion, states the failure condition in the name):
```java
@Test
void deleteStudent_idNotFound_throwsIllegalStateException() {
    given(studentRepository.existsById(99L)).willReturn(false);

    assertThatThrownBy(() -> studentService.deleteStudent(99L))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("does not exist");
}
```

## References

- Runner + commands: `docs/config/project-commands.yaml` (`test`/`test_unit`: `./mvnw test`).
- Canonical source to mirror in tests: `src/main/java/com/example/demo/student/{Student,StudentController,StudentService,StudentRepository}.java`.
- Stack decision record: `docs/adr/0001-tech-stack.md`.

<!-- ============================================================================ -->
<!-- OPTIONAL — keep only what applies to junit; DELETE the rest.            -->
<!-- ============================================================================ -->
