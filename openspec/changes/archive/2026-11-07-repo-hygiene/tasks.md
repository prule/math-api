# Tasks

## 1. Unused test dependencies

- [x] 1.1 Remove `spring-boot-starter-actuator-test` and `spring-boot-starter-validation-test` from `build.gradle.kts`; verify `./gradlew clean build` passes with the same test count as before (175), and that `./gradlew dependencies --configuration testRuntimeClasspath` no longer lists either starter

## 2. README

- [x] 2.1 Write `README.md` (what it is, without naming or counting operations: link PRD §3.1 and `/swagger-ui.html` for the list; build and run commands; one `curl` example using ADD; API docs paths and the `prod` profile; links to `docs/PRD.md`, `docs/DOMAIN-MODEL.md`, `docs/STANDARDS.md`, `docs/BACKLOG.md`; the CLAUDE.md change workflow); verify every command and path in it by running the app and the `curl` example, pasting the real response, and checking each linked file exists

## 3. Integration check

- [x] 3.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; verify `git diff --stat main` touches only `README.md`, `build.gradle.kts`, `docs/BACKLOG.md` and `openspec/`
