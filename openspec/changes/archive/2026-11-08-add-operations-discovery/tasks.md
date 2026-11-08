# Tasks

## 1. Operations endpoint and its OpenAPI entry

Both tests are written before the controller, because the endpoint and its documentation live in the same class.

- [x] 1.1 Write `adapter/in/web/OperationControllerTest` (`@WebMvcTest({OperationController.class, CalculationController.class})` + `@Import(UseCaseConfig.class)`), covering the `operation-discovery` scenarios: `GET /api/v1/operations` is 200 `application/json`; the `name` values equal `Operation.values()` names in order, and each entry's `arity` equals `Operation.arity()`; strict JSON for the `ADD`, `SQRT`, `NEGATE` entries (exactly `name` and `arity`, `arity` a number); every listed entry, posted to `/api/v1/calculations` with `a` `"4"` and `b` `"2"` only when arity is 2, returns 200; `POST /api/v1/operations` is 405 `application/problem+json`. Verify it fails to compile or fails (no controller yet).
- [x] 1.2 Add to `ApiDocumentationTest` a test for "Operations endpoint documented": path `/api/v1/operations` has a `get`; its responses are exactly `200`; the 200 content is only `application/json`; the schema is an array whose item (resolved through `$ref`) has properties exactly `name` (string) and `arity` (integer). Verify it fails.
- [x] 1.3 Add `adapter.in.web.OperationResponse(String name, int arity)` record with `@Schema` descriptions, and `OperationController` (`GET /api/v1/operations` returning `Arrays.stream(Operation.values()).map(...)`), with `@io.swagger.v3.oas.annotations.Operation(summary = ...)` and one `@ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = OperationResponse.class))))`. Verify 1.1 and 1.2 pass with `./gradlew test`.

## 2. Docs

- [x] 2.1 `docs/PRD.md`: §3.2 adds `GET /api/v1/operations` with its response example and notes it returns only 200 (other methods 405); §5 removes the operations-discovery non-goal; §6 strikes the `GET /operations` row as delivered in `add-operations-discovery`; change log row. Verify by reading the diff against the `operation-discovery` spec.
- [x] 2.2 `docs/STANDARDS.md`: §4 rewords "Numeric values in responses are JSON strings" to cover decimal values (Operands, Results), with counts such as `arity` as JSON integers; change log row. Verify by reading the diff.
- [x] 2.3 `README.md`: next to the "supported operations" pointer, mention `GET /api/v1/operations`. Verify by reading the diff.

## 3. Verify

- [x] 3.1 `./gradlew clean build` passes (tests, ArchUnit, Spotless; run `./gradlew spotlessApply` first if formatting fails).
- [x] 3.2 `openspec validate add-operations-discovery --strict` passes.

## Workflow follow-up

- Code review (ponytail + `/code-review`), archive (creates `openspec/specs/operation-discovery/`), retrospective, PR, merge on green, per `CLAUDE.md`.
