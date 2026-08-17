# ADR-0003: Upgrade Spring Boot to 3.3.5 for RestClient

- Status: Accepted
- Date: 2026-08-17
- Deciders: project lead

## Context

A new `SchoolClient` was added to call the School service's REST API (`http://localhost:8081/api/v1/school/{id}`) using `org.springframework.web.client.RestClient`. `RestClient` was introduced in Spring Framework 6.1, shipped starting with Spring Boot 3.2. The project was pinned to Spring Boot 3.0.6 (Spring Framework 6.0.x) per [ADR-0001](0001-tech-stack.md), so the code would not compile as-is.

## Decision

- Bump `spring-boot-starter-parent` from `3.0.6` to `3.3.5` in `pom.xml`. `java.version` (21) and all other dependencies are unchanged and remain compatible.

## Alternatives considered

- Use `RestTemplate` instead of `RestClient`, keeping Spring Boot 3.0.6: rejected — user explicitly chose to upgrade Spring Boot rather than swap the HTTP client API.

## Consequences

- Positive: `RestClient` is available; existing Student CRUD code and tests required no changes and all pass under 3.3.5.
- Negative: stack now diverges from the version recorded in ADR-0001 — ADR-0001 should be read alongside this ADR for the current Spring Boot version.
- Reversible? Low-medium cost — a further Spring Boot version change would need the same compile/test verification performed here.
