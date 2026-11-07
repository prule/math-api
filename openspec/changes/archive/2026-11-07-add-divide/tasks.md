# Tasks

## 1. Domain: DIVIDE and BR-2

- [x] 1.1 Add failing DIVIDE cases to `OperationTest`: parameterised rows for `1 ÷ 4`, `1 ÷ 3`, `2 ÷ 3`, `1 ÷ -4`, `0 ÷ 5`; tie cases `1.0000000000000000000000000000000005 ÷ 1` → `1` and `…0015 ÷ 1` → `1.000000000000000000000000000000002` asserted via `stripTrailingZeros().toPlainString()`; and a parameterised test that `0`, `0.00`, `-0`, `0E+5` divisors and `0 ÷ 0` throw `DivisionByZeroException`; verify `./gradlew test` fails only because `DIVIDE`/`DivisionByZeroException` don't exist (design D1, D4)
- [x] 1.2 Add `domain.DivisionByZeroException` and the `DIVIDE` constant (zero check, then `divide(b, MathContext.DECIMAL128)`) with BR-2/BR-3 Javadoc; verify `OperationTest` and `ArchitectureTest` pass (D1, D2)

## 2. Web: 422 mapping

- [x] 2.1 In `CalculationControllerTest`, replace `DIVIDE` with `MODULO` in `rejectsUnknownOperation`, add DIVIDE happy-path rows (`1 ÷ 3`, `1 ÷ 4`), and add a failing parameterised 422 test over divisors `"0"`, `"0.00"`, `"-0"`, `"0E+5"` and JSON number `0.0` using `expectProblem(…, 422, "/problems/division-by-zero")`; verify only the 422 test fails (it gets 500 today) (D4)
- [x] 2.2 Add the `DivisionByZeroException` handler to `ProblemDetailsAdvice` (422, type `/problems/division-by-zero`, title "Division by zero", fixed detail); verify all `CalculationControllerTest` tests pass (D3)

## 3. Docs

- [x] 3.1 Add a change log row to `docs/DOMAIN-MODEL.md` (DIVIDE implemented with BR-2/BR-3) and update §2 so `DivisionByZeroException` is no longer listed as "later"; verify §3's DIVIDE edge cases all appear in `OperationTest`
- [x] 3.2 Set backlog item 3 to Done with change name `add-divide` in `docs/BACKLOG.md`; verify the status table shows it

## 4. Integration check

- [x] 4.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; then `./gradlew bootRun` and verify via `curl` that `DIVIDE 1 ÷ 3` returns `"0.3333333333333333333333333333333333"`, `DIVIDE 1 ÷ 0` returns 422 `application/problem+json` with type `/problems/division-by-zero`, and an unknown name's detail lists `ADD, SUBTRACT, MULTIPLY, DIVIDE`

## Workflow follow-up

- Archive with `/opsx:archive`, then PR from a feature branch and merge once CI passes.
