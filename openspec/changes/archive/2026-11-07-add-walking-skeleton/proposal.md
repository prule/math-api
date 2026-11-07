# Proposal

## Why

The repository holds only documentation. Before any calculation behaviour is built, the build, the clean-architecture layer rules, formatting and CI must exist and pass, so every later change (backlog items 2–5) only adds behaviour and is checked by the same gates from its first commit.

## What Changes

- Add a Gradle (Kotlin DSL) build with the committed wrapper, a Java 25 toolchain and Spring Boot 4 (web MVC, validation, actuator).
- Add the base package `com.paulrule.mathapi` with an application entry point and the empty layer packages `domain`, `application`, `adapter.in.web` and `config`.
- Expose the service health check at `GET /actuator/health`. No other actuator endpoint is exposed over HTTP.
- Add Spotless with palantir-java-format, enforced by `./gradlew build`.
- Add ArchUnit tests that enforce the STANDARDS.md §2 dependency rules, run by `./gradlew build`.
- Add one `@SpringBootTest` smoke test: the context starts and the health check reports UP.
- Add a GitHub Actions workflow that runs `./gradlew build` on pull requests and on pushes to `main`.
- Add a `.gitignore` for Gradle output, IDE files and `logs/`.

**Out of scope:** any calculation behaviour (the `Operation` enum, `/api/v1/calculations`, Problem Details handling), OpenAPI/springdoc, operand limits, Docker or deployment, and branch-protection settings on GitHub.

## Capabilities

### New Capabilities

- `service-health`: the externally visible health check that tells callers and orchestrators whether the service is up, and the rule that no other operational endpoint is exposed over HTTP.

### Modified Capabilities

None. No specs exist yet.

## Impact

- **Code:** new `build.gradle.kts`, `settings.gradle.kts`, Gradle wrapper, `src/main` and `src/test` trees, `src/main/resources/application.yaml`.
- **Dependencies:** Spring Boot 4.1.x starters (webmvc, validation, actuator, test), ArchUnit (JUnit 5 integration), Spotless Gradle plugin with palantir-java-format.
- **CI:** new `.github/workflows/build.yml`. It becomes the required check for later PRs.
- **API:** adds `GET /actuator/health` only. No `/api/v1` endpoints yet.
- **Docs:**
  - `docs/STANDARDS.md`: update §1 Stack with the versions this change pins, mark the CI workflow as present in §7, and add a change log entry.
  - `docs/BACKLOG.md`: set item 1 to In progress / Done.
  - `docs/PRD.md`: none.
  - `docs/DOMAIN-MODEL.md`: none.
