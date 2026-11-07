# math-api

Stateless Spring Boot 4 / Java 25 REST API for exact decimal arithmetic.

Read before working:
- `docs/PRD.md` — scope, API contract, non-goals, roadmap
- `docs/DOMAIN-MODEL.md` — glossary and business rules (BR-n)
- `docs/STANDARDS.md` — architecture, conventions, testing, workflow
- `docs/BACKLOG.md` — ordered OpenSpec changes; update an item's status when you start or archive it

## Keep the docs current

Any change that alters scope, the API contract, operations, domain terms or business rules, or engineering standards MUST update the matching doc(s) in `docs/` in the same change, including that doc's change log. Never renumber or reuse a BR id.

## Change workflow

Every change (usually one `docs/BACKLOG.md` item) goes through these steps in order. Run each step yourself, without waiting to be asked. Stop only where a step needs the user's decision: a material ambiguity while proposing, or a choice a review raises.

1. **Propose**: `/opsx:propose` with the item's prompt from `docs/BACKLOG.md`. Done when every planning artifact exists and `openspec validate <change> --strict` passes.
2. **Review the plan**: `/ponytail:ponytail-review` on the change's artifacts, checked against the code they describe. Apply every review point; done when the artifacts reflect each one and strict validation passes again.
3. **Apply**: create branch `feat/<change>` from an up-to-date `main`, set the item to In progress in `docs/BACKLOG.md`, then `/opsx:apply`. Done when every task is checked and `./gradlew clean build` passes (tests, ArchUnit, Spotless). Fix formatting with `./gradlew spotlessApply`; never weaken an ArchUnit rule or a test to get green.
4. **Review the code**: run both reviews on the uncommitted diff, then apply every point from either, each with a test that fails without it, and rerun `./gradlew build`.
   - `/ponytail:ponytail-review` on the diff and the code it touches: is it correct, safe, tested and minimal?
   - `/code-review` against `main`: does it follow `docs/STANDARDS.md` and these rules, and does it meet every scenario in the change's spec delta?
5. **Archive**: `/opsx:archive`. It syncs the delta specs into `openspec/specs/` (check with `openspec validate <capability> --type spec --strict`) and moves the change into `openspec/changes/archive/`. Set the item to Done in `docs/BACKLOG.md`.
6. **Retrospective**: run `/ponytail:ponytail-debt`, then write `retrospective.md` in the archived change's folder with four sections:
   - **Caught by review**: each finding from both reviews and how it was fixed.
   - **Surprises**: each failed check, spec deviation or bug found during apply or archive, with its cause, or "cause unknown".
   - **Shortcuts taken**: each `shortcut:` comment this change added, with its file, its limit and when to upgrade.
   - **Lessons**: what to do differently next time, each naming the step, file or habit it changes.

   Write "None" under a section with nothing to report.
7. **Commit and PR**: commit on `feat/<change>` with a Conventional Commits message, push, and open the PR with the `pr` skill, filling in `.github/pull_request_template.md`.
8. **Merge on green**: once the PR's CI `build` check passes, `gh pr merge <n> --squash --delete-branch`, then pull `main`. Never merge on a pending or failing check; `main` has no branch protection, so this rule is the gate.

Every review is mandatory. Ponytail and `/code-review` ask different questions: ponytail asks whether what was built is right and minimal; `/code-review` asks whether it matches what the repo has written down. Ponytail reviews have caught a test pinned to planned operation names, an unguarded `Integer.MIN_VALUE` edge in BR-1, mis-documented API responses, a speed test that could pass without running, and a duplicated limit.

## Milestone close

Before the first change of a new milestone (the first post-v1 item, 6, starts v2), run `/ponytail:ponytail-audit` on the whole repo. Turn each finding you act on into a backlog item or OpenSpec change, and note the ones you skip and why.
