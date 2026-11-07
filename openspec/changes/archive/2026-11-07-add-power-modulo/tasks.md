# Tasks

## 1. Domain: MODULO

- [x] 1.1 Add failing `OperationTest` rows for MODULO (`−7 mod 3 = −1`, `7 mod −3 = 1`, `5.5 mod 2 = 1.5`, `−7.5 mod 2 = −1.5`, `6 mod 3 = 0`, `1E+1000 mod 7 = 4`) and a test that `7 mod 0` and `7 mod 0.00` throw `DivisionByZeroException`; verify they fail only because `MODULO` doesn't exist (design D3)
- [x] 1.2 Add the `MODULO` constant (zero check, then `a.remainder(b)`) with BR-2/BR-7 Javadoc; verify `OperationTest` passes (D3)

## 2. Domain: POWER

- [x] 2.1 Add failing `OperationTest` cases for POWER: `2^10 = 1024`, `2^−2 = 0.25`, `3^−1` (34 threes), `(−2)^3 = −8`, `1.1^2 = 1.21`, `2^200` rounded (spec value), `0^0 = 1`, `5^0 = 1`, `2^3.0 = 8`; `InvalidExponentException` for `0.5`, `1000`, `−1000`, `1E+3`; `DivisionByZeroException` for `0^−1`; `InvalidExponentException` (not division by zero) for `0^−0.5`; `ResultOutOfRangeException` for `100^999` and `0.01^999`; `10^999` and `0.1^999` accepted; under `assertTimeoutPreemptively(1 s)`: the worst BR-1 operand (one hundred 9s, scale 1000) `^999` and `^−999` each throw `ResultOutOfRangeException`, and `1.0…01` (100 digits) `^999` returns `1`; verify they fail only on missing types (D1, D2)
- [x] 2.2 Add `domain.InvalidExponentException`, `domain.ResultOutOfRangeException` and the `POWER` constant (exponent check, zero-base check, exact power then `DECIMAL128`, normalised-scale bound) with BR-3/5/6 Javadoc; verify `OperationTest`, `CalculationTest` and `ArchitectureTest` pass (D1, D2, D4)

## 3. Web: errors and contract

- [x] 3.1 In `CalculationControllerTest`, add failing tests for every POWER/MODULO spec scenario through the API (results as strings), the two new 422 types via `expectProblem`, MODULO by zero, zero to a negative power, and the new validation-order scenario, and "Worst-case operands stay fast" with the operand in E notation (`"9.99…9E-901"`; written plainly it is 1,002 characters and would be rejected by the 1,000-character string limit before POWER runs), asserting the 422/200 outcomes within 1 second; verify only the new 422s fail (500 today) (D4)
- [x] 3.2 Add the two handlers to `ProblemDetailsAdvice` (422, fixed details, no values echoed); verify all `CalculationControllerTest` tests pass (D4)

## 4. API documentation

- [x] 4.1 In `ApiDocumentationTest`, extend the 422 example-types assertion to the three types, add a test that the `unknown-operation` example's listed operations equal `Operation.values()`, and confirm the allowed-values test now expects six names; verify the new assertions fail (D5)
- [x] 4.2 Add `ProblemExamples.INVALID_EXPONENT` and `RESULT_OUT_OF_RANGE` (real advice output), update `UNKNOWN_OPERATION` to six names, add the two `@ExampleObject`s and update the 422 description on the controller; verify `ApiDocumentationTest` passes (D5)

## 5. Docs

- [x] 5.1 Update `docs/DOMAIN-MODEL.md`: model diagram operations, §2 exception list, §3 rows for POWER and MODULO with their edge cases, BR-2 and BR-3 amended, new BR-5/BR-6/BR-7, change log row; verify every BR-5–7 example has a test (D6)
- [x] 5.2 Update `docs/PRD.md`: §3.1 rows, §3.2 error table (two 422 rows) and validation order line, §6 roadmap row marked delivered, change log row; verify the error table lists every `type` the advice can return (D6)

## 6. Integration check

- [x] 6.1 Run `./gradlew clean build` and verify all tests, ArchUnit and Spotless pass; then `./gradlew bootRun` and verify via `curl`: `POWER 2^10` → `"1024"`, `MODULO −7 mod 3` → `"−1"`, `POWER 2^0.5` → 422 `invalid-exponent`, and `/v3/api-docs` lists six operations
