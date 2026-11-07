# Tasks

## 1. Domain: Operation and Calculation

- [x] 1.1 Write failing `OperationTest` for every DOMAIN-MODEL.md §3 edge case of ADD, SUBTRACT and MULTIPLY (0.1 + 0.2; negatives; large precision; zero result; negative result; × 0; sign rules; scale growth), plus `Operation.named` accepting `"ADD"` and throwing `UnknownOperationException` for `"add"` and `"POWER"`; verify `./gradlew test` fails only because the types don't exist yet (design D1)
- [x] 1.2 Implement `domain.Operation` (constants implementing `apply`, `named`) and `domain.UnknownOperationException` carrying the rejected name; verify `OperationTest` passes (D1)
- [x] 1.3 Write failing `CalculationTest` covering BR-4 (`0.10 × 10` → `1`, `1.10 − 1.1` → `0` with scale 0, `1000 × 1000` has no trailing zeros and `toPlainString()` is `"1000000"`) and null rejection for each component; verify it fails for the missing type (D2)
- [x] 1.4 Implement `domain.Calculation` record with null checks and `result()`; verify `CalculationTest` passes and `ArchitectureTest` still passes (domain imports only `java..`) (D2)

## 2. Application: PerformCalculation

- [x] 2.1 Write failing `PerformCalculationTest` asserting `perform` returns the Calculation's normalised Result; implement `application.PerformCalculation` (no Spring imports) and `config.UseCaseConfig` exposing it as a `@Bean`; verify the test and `ArchitectureTest` pass (D3)

## 3. Web adapter: happy path

- [x] 3.1 Write failing `CalculationControllerTest` (`@WebMvcTest(CalculationController.class)` + `@Import(UseCaseConfig.class)`) for the spec scenarios "Successful Calculation", "Number and string operands", "JSON number read exactly", "Scientific input echoed plainly", and one request per operation; verify it fails for the missing controller (D4, D6)
- [x] 3.2 Implement `adapter.in.web.CalculationRequest`, `CalculationResponse` and `CalculationController` (`POST /api/v1/calculations`, `toPlainString()` on a, b and result); verify the 3.1 tests pass. If string or exact-number binding fails under Jackson 3, add the minimal `JsonMapperBuilderCustomizer` in `config` and note it in design.md (D4, Risks)

## 4. Web adapter: Problem Details

- [x] 4.1 Add failing tests to `CalculationControllerTest` for every "Invalid request rejected" and "Unknown Operation rejected" scenario, each also asserting `application/problem+json`, `type`, `title`, `status` matching, and a `detail` with no `Exception`/`tools.jackson` text; verify they fail on type/shape, not on compile errors (D5)
- [x] 4.2 Implement `adapter.in.web.ProblemDetailsAdvice` (`@RestControllerAdvice` extending `ResponseEntityExceptionHandler`, overriding message-not-readable and argument-not-valid, plus an `UnknownOperationException` handler); verify all `CalculationControllerTest` tests pass (D5)

## 5. Smoke test and architecture rules

- [x] 5.1 Extend `ServiceHealthSmokeTest` with one happy-path `POST /api/v1/calculations` (e.g. `MULTIPLY` `"1.5"` × `"1.5"` → `"2.25"`) through the full context; verify it passes (D6)
- [x] 5.2 Remove `allowEmptyShould(true)`, `withOptionalLayers(true)` and the `shortcut:` comment from `ArchitectureTest`; verify all four `@ArchTest` rules and `ArchitectureRulesAreLiveTest` pass against the now-populated layers (D7)

## 6. Docs

- [x] 6.1 Update `docs/PRD.md` §3.2 (operation names exact and case-sensitive; `a`/`b` echoed in plain notation with their given scale) and add a change log row; verify the PRD example still matches the spec's response shape
- [x] 6.2 Update `docs/DOMAIN-MODEL.md` §2 (list `UnknownOperationException`; Operation names exact and case-sensitive) and add a change log row; verify the glossary terms used in code (`Operation`, `Calculation`, Result) match the doc
- [x] 6.3 Set backlog item 2 to Done with change name `add-exact-arithmetic` in `docs/BACKLOG.md`; verify the status table shows it

## 7. Integration check

- [x] 7.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; then `./gradlew bootRun` and verify `curl -s -H 'Content-Type: application/json' -d '{"operation":"ADD","a":0.1,"b":"0.2"}' localhost:8080/api/v1/calculations` returns `result` `"0.3"` and `{"operation":"POWER","a":1,"b":2}` returns 400 `application/problem+json` with type `/problems/unknown-operation`

## Workflow follow-up

- Open a PR from a feature branch, merge once CI passes.
- Archive the change with `/opsx:archive`.
