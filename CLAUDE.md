# my-app

harness scaffolded by harness init

@README.md
@docs/config/project-commands.yaml

## Tech stack

- `spring-boot-web` — Spring Boot 3.0.6, Java 21, Maven (`mvnw`), JUnit 5 (`spring-boot-starter-test`), PostgreSQL via Spring Data JPA. See `docs/adr/0001-tech-stack.md`.

## Integrations

- Document tracker: `local`
- Issue tracker: `none`
- VCS: `github`
- CI: `github-actions`

## SDLC

State machine: `docs/state/features.json` (index, pre-plan) + `docs/features/<id>/state.json` (per-feature, post-plan). See `docs/state/SCHEMA.md` for the two-tier shape. Gated by `phase-preconditions` skill.

Greenfield: `/arh-init` → `/arh-scaffold` → `/arh-intake` → `/arh-validate-story` → `/arh-research` → `/arh-plan-requirements` → `/arh-plan-implementation` → `/arh-implement` → `/arh-validate-feature` → `/arh-review` → `/arh-security-review`.

Brownfield: `/arh-init` → `/arh-import --jira-jql "..."` → continue per feature.

Helpers: `/arh-trace`, `/arh-explain <id>`, `/arh-sync`, `harness carry-forward {list|resolve|defer}`.

## Where to look

| What | Where |
|---|---|
| Build / test / dev commands | `docs/config/project-commands.yaml` (auto-imported above) |
| Stack idioms + anti-patterns + design tokens | `.claude/skills/<framework>-patterns/SKILL.md` (one per declared stack) |
| Cross-cutting rules (security, a11y, perf, reusability) | `.claude/rules/*-baseline.md` (auto-loaded, path-scoped) |
| Architectural decisions | `docs/adr/<NNNN>-<slug>.md` |
| Per-feature artefacts (story → research → PRD → PLAN → review) | `docs/features/<id>/` |
| Tracker config | `docs/config/{issue-tracking,doc-tracker}.yaml` |

<!-- Harness scaffold — sections below filled by /arh-init Phase 4 (bootstrap-agent) -->

## Conventions

- None beyond the default `.claude/rules/*-baseline.md` rules — user opted to keep defaults only, no extra project-specific conventions.

## Personas

- None — solo tutorial/learning project, developer-only, no end-user personas to model.

## Domain glossary

- None — generic CRUD tutorial (Student entity: id/name/email/dob) with no real-world domain framing.

## Target platforms

- Server/cloud container (Linux) — Java 21 runtime, Spring Boot jar built via Maven.

## Branch conventions

- `feature/<id>`, `bugfix/<id>`, `hotfix/<id>`, `chore/<id>`.

## Commit format

- Conventional Commits: `type(scope): summary`.

## PR conventions

- Title: `type(scope): summary`. Body sections: Summary, Test plan, Migration (when applicable).
