# spring-boot-web

- Deps: [NEEDS CLARIFICATION — no start command for Postgres] (app connects to `jdbc:postgresql://localhost:5432/student`, credentials in `src/main/resources/application.properties`; no docker-compose, Dockerfile, or documented DB bootstrap exists in this repo)
- Migrate: (n/a — no migration tool; `spring.jpa.hibernate.ddl-auto=create-drop` recreates schema from JPA entities on each app start/stop — tutorial-scope gap, see ADR-0002)
- Run: ./mvnw spring-boot:run
- Docker: (n/a — no Dockerfile in this repo)
- Check: http://127.0.0.1:8080/api/v1/student
  <!-- No Spring Boot Actuator dependency is present, so there is no /health endpoint.
       Falling back to the one real, unauthenticated GET route (`api/v1/student`,
       base path from StudentController) as the closest smoke-check substitute.
       server.port is unset in application.properties, so the default 8080 applies. -->
