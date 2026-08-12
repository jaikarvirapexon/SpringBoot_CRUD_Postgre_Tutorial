---
name: vcs-github
description: GitHub VCS operations — PR creation, status checks, releases. Uses gh CLI plus the GitHub MCP server.
when_to_use: Opening PRs, fetching PR diffs / metadata, or triggering releases.
user-invocable: false
allowed-tools: Bash mcp__github__*
---
# VCS — GitHub

Required env: `GITHUB_TOKEN` (or run via `gh auth login` for the local user).

## Operations

<!-- Harness scaffold: integration=github (vcs) -->

- Open PR → `gh pr create --title "..." --body-file body.md`
- Fetch PR → `gh pr view <num> --json title,body,headRefName,files`
- Diff → `gh pr diff <num>`
- Checks → `gh pr checks <num>`
- Release → `gh release create v<X> --notes-file CHANGELOG.md`

## PR body convention

- No `.github/pull_request_template.md` exists yet — write the body directly via `gh pr create --body-file`.
- Per `CLAUDE.md` § PR conventions: title `type(scope): summary`; body sections Summary, Test plan, Migration (when applicable).

## Branch protection

- No branch protection rule is configured on `main` for this repo (`jaikarvirapexon/SpringBoot_CRUD_Postgre_Tutorial`) — single-committer tutorial repo, unverified via `gh`/MCP (neither exposes branch-protection read; confirm directly on GitHub if this changes).
- No `.github/workflows/` exist yet despite `ci: github-actions` in `harness.yaml` — there is no CI status check to require.
- Once CI exists: protect `main`, require the CI status check, squash-merge (keeps history linear, matches the Conventional Commits PR-title convention above).
