# Tasks

## 1. Dependency and endpoints

- [x] 1.1 Write failing `ApiDocumentationTest` (`@SpringBootTest` + `@AutoConfigureMockMvc`) for "Document available" (`GET /v3/api-docs` → 200, `openapi` starts with `3.1`) and "UI reachable" (`/swagger-ui.html` redirects to a 200 HTML page); verify it fails with 404s (design D5)
- [x] 1.2 Add `org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0` to `build.gradle.kts` and `springdoc.api-docs.path`, `springdoc.api-docs.version: openapi-3-1`, `springdoc.swagger-ui.path` to `application.yaml`; verify the 1.1 tests pass and `ArchitectureTest` still passes (D1)

## 2. Calculation contract in the document

- [x] 2.1 Add failing tests to `ApiDocumentationTest` for "Path and response shape" (200 under `application/json`, not `*/*`), "Operand types" (number and string, required list) and "Allowed values match supported Operations" (expected list computed from `Operation.values()`); verify they fail on the missing schema details (D5)
- [x] 2.2 Add `@Schema` annotations to `CalculationRequest` and `CalculationResponse` (D3), a `@io.swagger.v3.oas.annotations.Operation(summary = …)` and an explicit `@ApiResponse` for 200 (`application/json`, `CalculationResponse`) on the controller method (D4), and `config.OpenApiConfig` with the `OpenAPI` info bean and the `OpenApiCustomizer` filling `operation`'s enum from `Operation.values()` (D2); verify the 2.1 tests pass. If springdoc renders the number-or-string type differently than expected, adjust the assertion to the actual shape and record it in design.md (Risks)

## 3. Error responses in the document

- [x] 3.1 Add failing tests to `ApiDocumentationTest` for "Each error type has an example" (400 has `application/problem+json` examples with the three `type`s; 422 has `division-by-zero`) and "Only the real responses are documented" (response codes exactly `200`, `400`, `422`); verify they fail (D4a, D5)
- [x] 3.2 Add `adapter.in.web.ProblemExamples` (example bodies matching the advice's real output) and `@ApiResponse` declarations for 400 and 422 on the controller method (D4); verify the 3.1 tests and all existing tests pass. If the exact-codes test fails because springdoc adds generic responses from the advice, set `springdoc.override-with-generic-response: false` in `application.yaml` and record what it had added in design.md (D4a)

## 4. Docs

- [x] 4.1 Update `docs/STANDARDS.md` §1 (springdoc 3.2.0 pinned, no longer "added in backlog item 5") and §4 (generated OpenAPI docs must cover every endpoint and error type, enforced by `ApiDocumentationTest`), plus a change log row; verify the version matches `build.gradle.kts`
- [x] 4.2 Update `docs/PRD.md` §3.3 (OpenAPI 3.1, operation values generated from the enum) and add a change log row; set backlog item 5 to Done with change name `add-api-docs` in `docs/BACKLOG.md`; verify both docs show it

## 5. Integration check

- [x] 5.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; then `./gradlew bootRun`, verify `curl -s localhost:8080/v3/api-docs` contains `/api/v1/calculations` and `division-by-zero`, and `curl -sIL localhost:8080/swagger-ui.html` ends in `200`

## Workflow follow-up

- Review, archive with `/opsx:archive`, then PR from a feature branch and merge once CI passes.
