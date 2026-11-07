# Tasks

## 1. Disabled mode under the prod profile

- [x] 1.1 Write failing `ApiDocumentationDisabledTest` (`@SpringBootTest` + `@AutoConfigureMockMvc` + `@ActiveProfiles("prod")`) for "Disabled under the prod profile" (404 for `/v3/api-docs`, `/swagger-ui.html`, `/swagger-ui/index.html`) and "Calculation API unaffected when docs are disabled" (`MULTIPLY "1.5" × "1.5"` → `"2.25"`, health `UP`); verify the 404 assertions fail (docs are still served) (design D3)
- [x] 1.2 Add `src/main/resources/application-prod.yaml` with `springdoc.api-docs.enabled: false` and `springdoc.swagger-ui.enabled: false` and a one-line why comment; verify `ApiDocumentationDisabledTest` passes and the unchanged `ApiDocumentationTest` still passes ("Enabled by default") (D1, D2)
- [x] 1.3 Check `GET /webjars/swagger-ui/index.html` under `prod` (temporary assertion or `bootRun --spring.profiles.active=prod` + `curl`) and record the result in design.md Risks; verify the note is written (Risks)
- [x] 1.4 Block the webjar under `prod` (user decision on 1.3): add `/webjars/swagger-ui/index.html` to `ApiDocumentationDisabledTest`'s 404 paths and verify it fails, then add `spring.web.resources.add-mappings: false` to `application-prod.yaml`; verify all `ApiDocumentationDisabledTest` and `ApiDocumentationTest` tests pass (Risks)

## 2. Docs

- [x] 2.1 Update `docs/PRD.md` §3.3 (docs on by default, off with `SPRING_PROFILES_ACTIVE=prod`; individual switches overridable) and add a change log row; verify it names the profile and both springdoc properties
- [x] 2.2 Add `docs/STANDARDS.md` §8 "Configuration & profiles" (`application.yaml` = local-development defaults; environment overrides in `application-<profile>.yaml`; `prod` disables API docs), renumber the change log to §9, and add a change log row; verify the section numbers are sequential
- [x] 2.3 Add this change to `docs/BACKLOG.md` as a Done follow-up after item 5 (`add-api-docs-toggle`); verify the status table shows it

## 3. Integration check

- [x] 3.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; then run `./gradlew bootRun --args='--spring.profiles.active=prod'` and verify `curl -s -o /dev/null -w '%{http_code}' localhost:8080/v3/api-docs` prints `404` and a calculation still returns 200; stop it, run without the profile and verify `/v3/api-docs` returns 200

## Workflow follow-up

- Review, archive with `/opsx:archive`, then PR from a feature branch and merge once CI passes.
