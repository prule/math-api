# Tasks

## 1. Domain: BR-1

- [x] 1.1 Add failing tests to `CalculationTest`: limits exactly met (100-digit integer, `1E+1000`, `1E-1000`) construct fine; 101-digit integer, `1` + 100 zeros, `1E+1001`, `1E-1001` throw `OperandOutOfRangeException` with the right `operand()` (`a` and `b` each) and `limit()` (`DIGITS`/`SCALE`); `1e999999999` is rejected inside `assertTimeoutPreemptively(Duration.ofSeconds(1), …)`; verify they fail only because the exception type doesn't exist (design D1, D2, D6)
- [x] 1.2 Add `domain.OperandOutOfRangeException` (operand name + nested `Limit { DIGITS, SCALE }`, message without the value) and the BR-1 check in `Calculation`'s compact constructor (`MAX_DIGITS = 100`, `MAX_SCALE = 1000`, `a` checked before `b`); verify `CalculationTest`, `OperationTest` and `ArchitectureTest` pass (D2)

## 2. Web: operand-out-of-range mapping

- [x] 2.1 Add failing tests to `CalculationControllerTest` for the spec scenarios "Exactly 100 digits accepted", "101 digits rejected", "Trailing zeros written out count as digits", "Scale limits accepted", "One past each scale limit rejected", "Extreme exponents rejected before any arithmetic" (under a 1-second `assertTimeoutPreemptively`), "Detail identifies operand and limit", and both "Validation order" scenarios, each error via `expectProblem`; verify the out-of-range ones fail on status/type (today they return 200 or 500) (D6)
- [x] 2.2 Add the `OperandOutOfRangeException` handler to `ProblemDetailsAdvice` (400, `/problems/operand-out-of-range`, title "Operand out of range", detail naming the operand and limit); verify all `CalculationControllerTest` tests pass (D5)

## 3. Bounded request parsing

- [x] 3.1 Add failing tests to `CalculationControllerTest` for "Oversized body" (> 16,384 chars via a padding field), "Oversized string operand" (1,001-digit string), "Oversized number operand" (1,001-digit number) and "Large but parseable operand reaches the domain rule" (150-digit string → operand-out-of-range), each via `expectProblem`; verify the oversized-body and oversized-string tests fail (no limits configured yet) (D4, D6)
- [x] 3.2 Add `spring.jackson.factory.constraints.read` (`max-document-length: 16384`, `max-string-length: 1000`, `max-number-length: 1000`) to `src/main/resources/application.yaml` with a one-line why comment; verify all 3.1 tests pass. If they don't, apply the design's fallback (`JsonFactoryBuilderCustomizer` and/or explicit `StreamConstraintsException` mapping) and record it in design.md (D4, Risks)

## 4. Docs

- [x] 4.1 Update `docs/DOMAIN-MODEL.md`: BR-1's text states the limit applies to the Operand as written (replace "so `1e1000` is the largest power of ten accepted" with the accurate examples: `1E+1000` ok, `1E+1001` rejected, a 101-digit integer rejected — write `1E+100`); §2 lists `OperandOutOfRangeException` as implemented; add a change log row; verify every BR-1 example in the doc is covered by a test
- [x] 4.2 Update `docs/PRD.md` §3.2 with the request limits (16,384-char body, 1,000-char tokens → `invalid-request`) and the validation order, plus a change log row; verify the error table still lists every `type` the advice can return
- [x] 4.3 Set backlog item 4 to Done with change name `add-operand-limits` in `docs/BACKLOG.md`; verify the status table shows it

## 5. Integration check

- [x] 5.1 Run `./gradlew clean build` and verify tests, ArchUnit and Spotless pass; then `./gradlew bootRun` and verify via `curl` that `MULTIPLY 1e999999999 × 1e999999999` returns 400 `/problems/operand-out-of-range` in well under a second (`curl -w '%{time_total}'`), and a 20,000-character body returns 400 `/problems/invalid-request`

## Workflow follow-up

- Review, archive with `/opsx:archive`, then PR from a feature branch and merge once CI passes.
