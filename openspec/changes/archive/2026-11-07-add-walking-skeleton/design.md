# Design

## Context

The repository is greenfield. It has no build files and no Gradle or Maven installed globally. Java 25 (Temurin 25.0.4) is installed. Spring Initializr currently offers Spring Boot **4.1.2** as the latest release, with Java 25 support. Git is initialised on `main`. The layer rules, stack and conventions are defined in `docs/STANDARDS.md`. This design records only the choices that standard leaves open.

## Goals / Non-Goals

**Goals:**
- `./gradlew build` is the single gate: compile, tests, ArchUnit and the Spotless check all run in it, locally and in CI.
- The layer rules are enforced before any layer has real code, so the first behaviour change can't violate them unnoticed.

**Non-Goals:**
- No placeholder controller, use case or domain type to "exercise" the layers.
- No custom health indicators. The built-in health check is enough while the service is stateless.
- No Gradle version catalog, convention plugins or multi-module setup.

## Decisions

### D1. Generate the build from Spring Initializr, then trim

Generate with `start.spring.io`: Gradle Kotlin DSL, Java 25, Boot 4.1.2, group `com.paulrule`, artifact `math-api`, package `com.paulrule.mathapi`, with dependencies `web`, `validation`, `actuator`. That gives a correct Gradle wrapper (a Gradle version that supports Java 25) and the Boot 4 starter names, without a global Gradle install. Delete the generated `HELP.md` and the generated `*ApplicationTests` (replaced by D8). Keep `gradlew.bat` for Windows contributors.

*Alternatives:* hand-writing the build files is error-prone for Boot 4's renamed, modular starters (e.g. `spring-boot-starter-webmvc` and `spring-boot-starter-webmvc-test`), and it still needs a wrapper from somewhere. Installing Gradle via SDKMAN just to run `gradle wrapper` adds a machine dependency the repo doesn't need.

### D2. Java toolchain pinned to 25

`java { toolchain { languageVersion = JavaLanguageVersion.of(25) } }` keeps the bytecode level independent of whatever JDK runs Gradle, and makes CI use 25 explicitly.

### D3. Empty layer packages hold a `package-info.java`

Java has no empty packages. Each layer gets a `package-info.java` with a one-line Javadoc stating the layer's role and its allowed dependencies (from STANDARDS.md §2). That makes the structure visible in the IDE and gives ArchUnit something to import.

### D4. ArchUnit: explicit `noClasses()` rules, with empty-`should` allowed until the layers fill

Use ArchUnit's JUnit 5 integration (`@AnalyzeClasses(packages = "com.paulrule.mathapi")`, `@ArchTest` fields) with one rule per STANDARDS.md §2 bullet:

- `domain` depends on nothing outside `java..` and `domain`. This is an allow-list, not a deny-list, so a new framework can't slip in.
- `application` does not depend on `adapter..`, `config..`, `org.springframework.web..` or `org.springframework.http..`.
- `domain` and `application` do not depend on `adapter..` DTOs (this follows from the two rules above, so no separate rule).
- Nothing outside `config` and the application entry point is annotated `@Configuration`.
- A layered-architecture rule (`layeredArchitecture().consideringOnlyDependenciesInLayers()`) to catch any inward-to-outward edge.

ArchUnit fails a rule whose `that()` matches no classes (`failOnEmptyShould`). While the layers are empty, the rules set `.allowEmptyShould(true)`, with a `shortcut:` comment saying to remove it in backlog item 2 once each layer has a class. A test (D6) proves the rules really bite.

*Alternative:* skip ArchUnit until item 2. Rejected, because the backlog promises the gate exists from this change on.

### D5. Spotless runs as a check inside `build`, never auto-applies there

Use the Spotless Gradle plugin with `java { palantirJavaFormat(); removeUnusedImports(); trimTrailingWhitespace(); endWithNewline() }` and `kotlinGradle { ktlint() }` for the build scripts. `spotlessCheck` is part of `check`, so `build` fails on unformatted code. Developers run `./gradlew spotlessApply`. Pick the newest palantir-java-format that parses Java 25 syntax.

### D6. Prove the ArchUnit rules are live

A plain unit test runs the domain rule against a fixture class in `src/test` (e.g. `com.paulrule.mathapi.domain.fixture.UsesSpring` importing a Spring type), imported by `ClassFileImporter`, and asserts the rule **fails**. Without this, an "allow empty" rule could silently pass forever. The fixture lives only in test sources, so the production ArchUnit scan (`ImportOption.DoNotIncludeTests`) never sees it.

### D7. Actuator exposure: health only, no details

`application.yaml`:
```yaml
management:
  endpoints.web.exposure.include: health
  endpoint.health.show-details: never
```
These match Boot's defaults, but they're stated explicitly so that adding an actuator dependency later can't widen exposure silently, and so the spec's rules are visible in config.

### D8. Smoke and exposure tests use one `@SpringBootTest` with MockMvc

`@SpringBootTest` + `@AutoConfigureMockMvc` (Boot 4 package `org.springframework.boot.webmvc.test.autoconfigure`, from `spring-boot-starter-webmvc-test`) checks the context starts and covers every `service-health` scenario: health UP, no `components`, `/actuator/env` 404, and the `/actuator` index links. A single test class keeps to STANDARDS.md's "one smoke test". MockMvc avoids a real port and the `TestRestTemplate`/`RestTestClient` module churn in Boot 4.

### D9. CI workflow

`.github/workflows/build.yml` runs on `pull_request` and on `push` to `main`. It uses `actions/checkout`, `actions/setup-java` (Temurin 25) and `gradle/actions/setup-gradle` (with caching and wrapper validation), then runs `./gradlew build`. Actions are pinned to major versions. No matrix, no artifacts, no deploy.

### D10. `.gitignore`

Ignore `.gradle/`, `build/`, IDE files (`.idea/`, `*.iml`, `.vscode/`, `out/`, `bin/`), OS files (`.DS_Store`) and `logs/`. **Keep** `.claude/` committed, because it holds the OpenSpec commands the workflow depends on.

## Risks / Trade-offs

- [Boot 4 moved test auto-configuration packages and split starters] → Taking the Initializr output as the source of truth avoids this. If an import doesn't resolve, check the Boot 4 migration guide rather than adding older starters.
- [palantir-java-format or ArchUnit lagging Java 25 bytecode/syntax] → Use their latest releases. If ArchUnit can't read Java 25 class files, the build fails loudly, which is the right outcome. Fall back to compiling with `--release 25` on the newest supported ArchUnit, and record it in STANDARDS.md.
- [`allowEmptyShould(true)` hides a rule that never matches] → D6's negative test, plus the `shortcut:` note to remove it in item 2.
- [CI action major versions drift] → Accepted. Major pins get fixes, and Dependabot can be added later if wanted.

## Migration Plan

Not applicable: there's no existing deployment. Rollback = revert the change.

## Open Questions

- Exact palantir-java-format / ArchUnit / Spotless versions are chosen at implementation time (latest stable that supports Java 25) and recorded in STANDARDS.md §1. This doesn't affect specs or tasks.
