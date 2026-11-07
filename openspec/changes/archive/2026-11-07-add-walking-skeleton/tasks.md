# Tasks

## 1. Build scaffold

- [x] 1.1 Generate the project from start.spring.io (Gradle Kotlin DSL, Java 25, Boot 4.1.2, group `com.paulrule`, artifact `math-api`, package `com.paulrule.mathapi`, deps `web`, `validation`, `actuator`) into the repo root without overwriting `docs/`, `openspec/`, `.claude/`, `.github/` or `CLAUDE.md`; verify `./gradlew --version` runs on Java 25 and `git status` shows no changes to existing files (design D1)
- [x] 1.2 Set the Java 25 toolchain in `build.gradle.kts`, delete `HELP.md` and the generated `*ApplicationTests`; verify `./gradlew compileJava` succeeds (D1, D2)
- [x] 1.3 Add `.gitignore` (Gradle, IDE, OS files, `logs/`; keep `.claude/`); verify `git status --ignored` lists `build/`, `.gradle/` and `logs/` as ignored and `.claude/` as untracked, not ignored (D10)

## 2. Health check (spec: service-health)

- [x] 2.1 Write the failing `@SpringBootTest` + MockMvc smoke test covering every `service-health` scenario: health 200 with `status` UP and no credentials, no `components`/`details` field, `GET /actuator/env` returns 404, and `GET /actuator` links only `self`, `health`, `health-path`; verify it fails for the expected reason (missing test starter/config), not a compile error in unrelated code (D8)
- [x] 2.2 Add `spring-boot-starter-webmvc-test` if Initializr didn't, and set actuator exposure and `show-details: never` in `src/main/resources/application.yaml`; verify the smoke test from 2.1 passes (D7, D8)

## 3. Layer packages and architecture rules

- [x] 3.1 Write the ArchUnit negative test first: a test-only fixture class under `com.paulrule.mathapi.domain` that imports a Spring type, checked against the domain rule through `ClassFileImporter`; verify it fails because the rule doesn't exist yet (D6)
- [x] 3.2 Add the ArchUnit (JUnit 5) dependency and the `@AnalyzeClasses` rule class with one `@ArchTest` per STANDARDS.md §2 rule (domain allow-list, application restrictions, `@Configuration` only in `config`/entry point, layered architecture), using `allowEmptyShould(true)` with a `shortcut:` comment pointing to backlog item 2; verify the 3.1 negative test now passes (the rule rejects the fixture) and the production-scan rules pass (D4, D6)
- [x] 3.3 Add `package-info.java` with a one-line role/dependency Javadoc to `domain`, `application`, `adapter.in.web` and `config`; verify `./gradlew test` passes and the four packages exist under `src/main/java/com/paulrule/mathapi/` (D3)

## 4. Formatting

- [x] 4.1 Add the Spotless plugin (palantir-java-format, remove unused imports, trim whitespace, final newline; ktlint for `*.gradle.kts`) and wire `spotlessCheck` into `check`; verify that adding a mis-indented line makes `./gradlew build` fail, then `./gradlew spotlessApply` fixes it and `./gradlew build` passes (D5)

## 5. Continuous integration

- [x] 5.1 Add `.github/workflows/build.yml` (on `pull_request` and `push` to `main`; checkout, setup-java Temurin 25, setup-gradle with wrapper validation, `./gradlew build`); verify the YAML parses (e.g. `python3 -c 'import yaml,sys; yaml.safe_load(open(sys.argv[1]))' .github/workflows/build.yml`) and that its steps match what `./gradlew build` needs locally (D9)

## 6. Docs

- [x] 6.1 Update `docs/STANDARDS.md` §1 with the pinned versions (Spring Boot, Gradle wrapper, ArchUnit, Spotless, palantir-java-format), note in §7 that the CI workflow now exists, and add a change log row; verify the versions match `build.gradle.kts` and `gradle/wrapper/gradle-wrapper.properties`
- [x] 6.2 Set backlog item 1 to Done with change name `add-walking-skeleton` in `docs/BACKLOG.md`; verify the status table shows it

## 7. Integration check

- [x] 7.1 From a clean checkout state (`./gradlew clean build`), verify the full build passes with tests, ArchUnit and Spotless all running (check the task list in the output includes `spotlessCheck` and `test`), and `./gradlew bootRun` + `curl -s localhost:8080/actuator/health` returns `{"status":"UP"}`

## Workflow follow-up

- Push to GitHub and confirm the `build` workflow passes on the first PR.
- Archive the change with `/opsx:archive` after review.
