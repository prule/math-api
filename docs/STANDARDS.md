# Math API — Engineering Standards

> Living document. Update it in the same change as any change to stack, structure, tooling or conventions (see `CLAUDE.md`).
> Related: [PRD](PRD.md) · [Domain Model](DOMAIN-MODEL.md)

"MUST" rules are enforced in review or by the build. "SHOULD" rules may be broken with a stated reason in the PR.

## 1. Stack

| Concern | Choice |
|---------|--------|
| Language | Java 25 (Gradle toolchain) |
| Framework | Spring Boot 4.1.2 (webmvc, validation, actuator) |
| Build | Gradle 9.8.2, Kotlin DSL, wrapper committed (`./gradlew`) |
| API docs | springdoc-openapi (version compatible with Boot 4; added in backlog item 5) |
| Tests | JUnit (Boot-managed version), AssertJ, Spring Boot Test, ArchUnit 1.5.1 |
| Formatting | Spotless 8.11.0 + palantir-java-format 2.103.0 (Java); ktlint (`*.gradle.kts`) |
| Group / base package | `com.paulrule` / `com.paulrule.mathapi` |

New dependencies MUST be justified in the OpenSpec change that adds them.

## 2. Architecture — clean architecture, single module

```
com.paulrule.mathapi
├── domain            entities, value objects, Operation enum, domain exceptions
├── application       use cases (e.g. PerformCalculation), ports
├── adapter.in.web    controllers, request/response DTOs, Problem Details mapping
└── config            Spring wiring (@Configuration), the only place use cases become beans
```

Dependency rule — arrows point inwards only:

```
adapter.in.web ──► application ──► domain
config ──────────► all
```

- `domain` MUST NOT import Spring, Jakarta, Jackson or any framework. Plain Java only.
- `application` MUST NOT import Spring web or adapter types. Use cases are plain classes wired in `config`.
- `adapter.in.web` MUST NOT contain business rules; it parses, delegates, maps.
- DTOs MUST NOT leak into `application` or `domain`; map at the adapter boundary.
- These rules MUST be enforced by ArchUnit tests that run in `./gradlew build`.
- Future outbound adapters (e.g. persistence) go in `adapter.out.<name>` behind a port declared in `application`.

## 3. Domain conventions

- Numbers are `BigDecimal`; never `double`/`float` for values.
- Business rules are documented in [DOMAIN-MODEL.md](DOMAIN-MODEL.md) with an ID (`BR-n`). Code enforcing a rule and its tests SHOULD cite the ID in the test name or a comment.
- Adding an operation: add the `Operation` constant, document its rules in DOMAIN-MODEL.md, add tests. Nothing else should need to change; if something does, the design has drifted — fix it.
- Domain failures are domain exceptions, never HTTP-aware.

## 4. API conventions

- Base path `/api/v{n}`. Adding an operation is non-breaking. Breaking changes → new major path version.
- Request/response bodies are Java records.
- Numeric values in responses are JSON strings (plain notation); requests accept numbers or strings.
- Errors MUST be RFC 9457 Problem Details via Spring `ProblemDetail`, from a single `@RestControllerAdvice`. Each error has a stable `type` URI listed in the PRD.
- Status codes: `400` invalid input; `422` valid input that violates a domain rule.
- Every endpoint MUST appear correctly in the generated OpenAPI docs.

## 5. Code style

- Spotless (palantir-java-format) MUST pass; `./gradlew spotlessApply` before committing.
- Use records for DTOs and value objects. No Lombok.
- Prefer immutability; no setters in domain.
- Comment the *why*, not the *what*.
- No speculative abstraction: build what the current PRD scope needs.

## 6. Testing

Write tests first (red → green → refactor).

| Level | Tool | Must cover |
|-------|------|-----------|
| Domain unit | JUnit + AssertJ | Every operation and every edge case listed in DOMAIN-MODEL.md; every business rule |
| Web slice | MockMvc web-slice test | Controller mapping, request parsing, every Problem Details error path in the PRD |
| Smoke | `@SpringBootTest` (one) | Context starts; one happy-path request end to end |
| Architecture | ArchUnit | Section 2 dependency rules |

- No coverage-percentage gate. Instead: every operation, every business rule and every error path MUST have a test.
- Test names describe behaviour: `dividesOneByThreeToThirtyFourSignificantDigits`.

## 7. Workflow

- Changes are planned with OpenSpec (`/opsx:propose` → `/opsx:apply` → `/opsx:archive`). Specs live in `openspec/specs/`.
- Commits follow Conventional Commits (`feat:`, `fix:`, `docs:`, `test:`, `refactor:`, `chore:`).
- Hosted on GitHub. PRs use `.github/pull_request_template.md`.
- CI (GitHub Actions, `.github/workflows/build.yml`) MUST run `./gradlew build` (tests, ArchUnit, Spotless) on every PR and push to `main`.
- **Docs stay current:** a change that alters scope, API, operations, domain rules or these standards MUST update PRD.md, DOMAIN-MODEL.md or STANDARDS.md in the same PR, including the doc's change log.

## 8. Change log

| Date | Change |
|------|--------|
| 2026-11-07 | Initial standards |
| 2026-11-07 | `add-walking-skeleton`: pinned stack versions in §1; CI workflow now exists (§7) |
