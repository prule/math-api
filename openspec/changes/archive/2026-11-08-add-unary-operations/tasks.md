# Tasks

## 1. Domain: arity (BR-9)

- [x] 1.1 Add failing `CalculationTest` cases: a binary Operation with a null `b` and a unary Operation with a non-null `b` each throw `ArityMismatchException` carrying the Operation; a unary Calculation with a null `b` is accepted; the arity check runs before BR-1 (`SQRT`, `a = 4`, `b = 1E+1001` throws `ArityMismatchException`, not `OperandOutOfRangeException`); replace the existing `CalculationTest` assertion that `ADD` with a null `b` throws `NullPointerException` with `ArityMismatchException` (that case is now the binary-without-`b` rule); and an `OperationTest` that `arity()` is 2 for the six existing Operations and 1 for `SQRT` and `NEGATE`. Verify they fail only on missing types/constants (design D1, D2)
- [x] 1.2 Add `domain.ArityMismatchException`, the `Operation(int arity)` constructor with `arity()` (each existing constant passes 2), and the arity check in `Calculation` before BR-1, skipping BR-1 for a null `b`. Verify `CalculationTest`, `OperationTest` and `ArchitectureTest` pass (D1, D2)

## 2. Domain: NEGATE and SQRT (BR-3, BR-4, BR-8)

- [x] 2.1 Add failing `OperationTest` rows: NEGATE `5 → −5`, `−2.50 → 2.50`, `0`, `−0`, `0.00 → 0` after normalisation; SQRT `4 → 2`, `0.25 → 0.5`, `1E+1000 → 1E+500`, `1E-1000 → 1E-500`, `2 → 1.414213562373095048801688724209698`, `10 → 3.162277660168379331998893544432719` (rounds up), ties to even (`1.00000000000000000000000000000000100000000000000000000000000000000025 → 1`, `1.00000000000000000000000000000000300000000000000000000000000000000225 → 1.000000000000000000000000000000002`), `0`, `−0`, `0.00 → 0`; `NegativeSquareRootException` for `−1` and `−0.0001`. Verify they fail only on missing constants/types (D4, D5)
- [x] 2.2 Add `domain.NegativeSquareRootException` and the `SQRT(1)` (sign check, then `a.sqrt(DECIMAL128)`) and `NEGATE(1)` (`a.negate()`) constants, with BR-3/BR-8 Javadoc. Verify `OperationTest` passes (D4, D5)

## 3. Web: request, response, errors

- [x] 3.1 In `CalculationControllerTest`, add failing tests for every new and modified `calculation` spec scenario: unary success with no `b` key (strict JSON), `"b": null` treated as omitted, `b` given to SQRT → 400 `invalid-request` whose detail contains `'b'`, `{ "operation": "SQRT" }` → 400, NEGATE and SQRT results as strings (incl. `1E+1000` → `"1"` plus 500 zeros, and the two tie cases), `-2.50` echoed, SQRT of a negative → 422 `negative-square-root` via `expectProblem`, and the three new validation-order scenarios. Confirm the existing "Missing operand" test (`ADD` with no `b`) still expects 400 with `'b'` in the detail. Verify only the new behaviour fails (D2, D3, D6)
- [x] 3.2 Drop `@NotNull` from `CalculationRequest.b` and update its description (required for binary Operations, omit for unary). Add `@JsonInclude(NON_NULL)` to `CalculationResponse.b` and update its description. Make the controller echo `b` only when present. Add `ProblemDetailsAdvice` handlers for `ArityMismatchException` (400 `invalid-request`, detail from the arity per D2) and `NegativeSquareRootException` (422, fixed detail), and reword the malformed-body detail so `b` reads as needed only by binary Operations. Verify all `CalculationControllerTest` tests pass (D2, D3, D6)

## 4. API documentation

- [x] 4.1 In `ApiDocumentationTest`, change the required-fields assertion to exactly `operation`, `a`; assert that `b`'s description mentions binary and unary; and add `/problems/negative-square-root` to the 422 example types. The allowed-values and unknown-example tests already derive from `Operation.values()`; leave them unchanged (never hard-code names). Verify the new assertions fail (D6)
- [x] 4.2 Add `ProblemExamples.NEGATIVE_SQUARE_ROOT` (the real advice output), update `UNKNOWN_OPERATION` to eight names and `INVALID_REQUEST` to the reworded malformed-body detail from 3.2, add the 422 `@ExampleObject` and update the 422 description on the controller, and change `OpenApiConfig`'s description from "two operands" to "one or two operands". Verify `ApiDocumentationTest` and `ApiDocumentationDisabledTest` pass (D6)

## 5. Docs

- [x] 5.1 Update `docs/DOMAIN-MODEL.md`: glossary (Operation of one or two Operands, Calculation's optional `b`, new **Arity** term), §2 model (`b` optional, `arity()`, new exceptions, the "adding an operation" line includes arity), §3 rows for SQRT and NEGATE with their edge cases, BR-3 widened to SQRT (exact: NEGATE), new BR-8 and BR-9, and a change log row. Verify every BR-8/BR-9 example has a test (D1, D4)
- [x] 5.2 Update `docs/PRD.md`: §1 (one or two operands), §3.1 rows and the "All operations are binary" line, §3.2 (a unary request/response example, the `b` rule, a 422 `negative-square-root` row, the validation-order line), §6 roadmap row marked delivered, and a change-log row noting the unknown-operation-without-`b` type change. Verify the error table lists every `type` the advice can return (D2, D6)
- [x] 5.3 Update `docs/STANDARDS.md` §"Adding an operation" to "add the `Operation` constant with its arity", plus a change-log row. Verify the line matches DOMAIN-MODEL.md §2 (D1)

## 6. Integration check

- [x] 6.1 Run `./gradlew clean build` and verify all tests, ArchUnit and Spotless pass. Then run `./gradlew bootRun` and verify with `curl`: `SQRT 2` → `"1.414213562373095048801688724209698"` with no `b` key, `NEGATE -2.50` → `"2.5"`, `SQRT -1` → 422 `negative-square-root`, `SQRT` with `b` → 400 `invalid-request`, `ADD 2 3` unchanged, and `/v3/api-docs` lists eight operations with only `operation` and `a` required

## Workflow follow-up

- Plan review (`/ponytail:ponytail-review`), code reviews, archive, retrospective, PR and merge per CLAUDE.md.
