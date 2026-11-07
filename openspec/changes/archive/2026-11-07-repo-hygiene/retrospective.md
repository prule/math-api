# Retrospective: repo-hygiene

Backlog item 5b, from the v2 milestone audit: a README for the public repo, and two unused test starters removed. `skip_specs` (no behaviour change); `design.md` skipped as optional, since there was no technical decision to record.

## Caught by review

- **Plan review:** the planned README would have said "the six operations" and listed them. Nothing keeps a root README current, because CLAUDE.md's docs rule covers `docs/` only, so item 7 (SQRT, NEGATE) would have left it wrong. Fixed: the README names and counts no operations, and links to PRD §3.1 and Swagger UI, which are already kept current. The `curl` example uses ADD.
- **Code review:** nothing found. Checked that the README's claims match the code (Java 25 is required, because no toolchain-download plugin is configured; the `curl` response is real; framework errors use the RFC's `about:blank` type), and that all 8 links and the PRD anchor resolve.

## Surprises

None. The two starters were confirmed unused again against the full 175-test suite. The audit's original check had only 114 tests.

## Shortcuts taken

None. The `ponytail-debt` scan found no `shortcut:` markers.

## Lessons

- **Step 1, propose:** any doc outside `docs/` (README, CLAUDE.md) should link to the living docs rather than restate lists or counts from them. If it has to restate something, add it to CLAUDE.md's "keep the docs current" rule in the same change.
- **Step 3, apply:** task 3.1's check that the diff touches only the expected files (`git diff --stat main`), added after item 6's screenshot mistake, worked. Keep it in every change's integration task.
