# Proposal

## Why

The v2 milestone audit (recorded in `docs/BACKLOG.md`) found two hygiene gaps:
- **No README.** github.com/prule/math-api is a public repo, but a visitor sees no description, no run command and no example.
- **Two unused test starters.** `spring-boot-starter-actuator-test` and `spring-boot-starter-validation-test` are on the test classpath, and all 114 tests passed without them during the audit. They're dependencies to download, update and audit for nothing.

## What Changes

- Add `README.md` at the repo root covering:
  - what math-api is: exact decimal arithmetic on two operands, with Problem Details errors. The README never names or counts the operations; it links to PRD §3.1 and `/swagger-ui.html`, which are already kept current, so it can't go stale when one is added.
  - how to build and run it: `./gradlew build`, `./gradlew bootRun`
  - one `curl` example with its real response
  - where the API docs are: `/swagger-ui.html` and `/v3/api-docs`, off under the `prod` profile
  - links to `docs/` (PRD, domain model, standards, backlog)
  - a pointer to the CLAUDE.md change workflow and OpenSpec
- Remove the two unused test dependencies from `build.gradle.kts`.
- **No behaviour change:** no API, spec or production-code changes. The change sets `skip_specs`.

**Out of scope:** CI or build changes beyond the two lines, badges, a contributing guide, licence selection, and branch-protection settings.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

None. This is documentation and build hygiene, so no spec-level behaviour changes (`skip_specs: true`).

## Impact

- **Code:** `build.gradle.kts` (−2 lines) and the new `README.md`. No production or test code changes.
- **Dependencies:** −2 test-only dependencies.
- **Docs:**
  - `docs/BACKLOG.md`: item 5b status.
  - `docs/PRD.md`: none.
  - `docs/DOMAIN-MODEL.md`: none.
  - `docs/STANDARDS.md`: none. Its stack table names the test libraries, not the starters, so it stays accurate.
