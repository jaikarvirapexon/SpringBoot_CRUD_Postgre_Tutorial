# ADR-0001: Tech stack

- Status: Accepted
- Date: 2026-08-12
- Deciders: project lead

## Context

This is a brownfield Spring Boot tutorial project: a working Student CRUD REST API already exists (Entity, Repository, Service, Controller, Config) backed by PostgreSQL via Spring Data JPA. The harness is being bootstrapped on top of this existing stack rather than choosing one from scratch.

## Decision

The harness records the following stack:

- Stacks: `spring-boot-web` — spring-boot v3.0.6 (Java 21, Maven)
- Package manager / Build / Test / Lint / Format: Maven (`mvnw`) / Maven / JUnit 5 (`spring-boot-starter-test`) / none configured / none configured
- Integrations: issue_tracker=none, doc_tracker=local, design=none, vcs=github, ci=github-actions

## Alternatives considered

- Gradle: rejected — project already uses Maven (`pom.xml`, `mvnw`); switching build tools is out of scope for harness bootstrap.
- Non-relational datastore: rejected — project already uses PostgreSQL via Spring Data JPA; no driver for this exists in the codebase.

## Consequences

- Positive: harness commands and architecture inference key off the actual, already-working stack — no drift between recorded stack and reality.
- Negative: no lint/format tooling is configured yet (flagged as a gap for `bootstrap-agent` / `project-commands`).
- Reversible? Low cost — this ADR only records the existing stack; it does not lock in new tooling choices.
