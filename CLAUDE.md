# math-api

Stateless Spring Boot 4 / Java 25 REST API for exact decimal arithmetic.

Read before working:
- `docs/PRD.md` — scope, API contract, non-goals, roadmap
- `docs/DOMAIN-MODEL.md` — glossary and business rules (BR-n)
- `docs/STANDARDS.md` — architecture, conventions, testing, workflow
- `docs/BACKLOG.md` — ordered OpenSpec changes; update an item's status when you start or archive it

## Keep the docs current

Any change that alters scope, the API contract, operations, domain terms or business rules, or engineering standards MUST update the matching doc(s) in `docs/` in the same change, including that doc's change log. Never renumber or reuse a BR id.

## Workflow

Plan changes with OpenSpec: `/opsx:propose` → `/opsx:apply` → `/opsx:archive`. Specs live in `openspec/specs/`.
Build and test: `./gradlew build`. Format: `./gradlew spotlessApply`.
