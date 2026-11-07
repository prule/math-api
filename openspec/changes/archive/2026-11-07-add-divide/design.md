# Design

## Context

`add-exact-arithmetic` built `Operation` as an enum whose constants implement `apply`. `Calculation.result()` applies BR-4 stripping, the controller renders with `toPlainString()`, and `ProblemDetailsAdvice` maps domain exceptions. The backlog asked to flag anything beyond "a new constant, an exception and its mapping". The only extra is in tests: `CalculationControllerTest.rejectsUnknownOperation` uses `DIVIDE` as an unknown name and must swap it out. No production code outside those three places changes.

## Goals / Non-Goals

**Goals:**
- BR-2 and BR-3 are enforced inside the `DIVIDE` constant, the one place a reader would look.

**Non-Goals:**
- No configurable `MathContext`. Client-chosen precision is a roadmap item, so the constant stays fixed until it's planned.
- No generic "domain rule violation" exception hierarchy. Wait until a second 422-type rule exists.

## Decisions

### D1. `DIVIDE.apply` checks the divisor, then uses `MathContext.DECIMAL128`

```java
DIVIDE {
    public BigDecimal apply(BigDecimal a, BigDecimal b) {
        if (b.signum() == 0) throw new DivisionByZeroException();  // BR-2
        return a.divide(b, MathContext.DECIMAL128);                 // BR-3
    }
}
```
- `signum() == 0` is true for every zero representation (`0`, `0.00`, `0E+5`). `BigDecimal` has no negative zero, so `"-0"` parses to zero too. No string comparison is needed.
- `MathContext.DECIMAL128` is exactly 34 digits with `HALF_EVEN`. Exact quotients that fit (`1 ÷ 4`) come back exact. `divide(b, mc)` never throws for non-terminating results, unlike `divide(b)`.
- The check comes *before* `divide`, so `BigDecimal`'s own `ArithmeticException("Division by zero")` (or "Division undefined" for `0 ÷ 0`) never escapes as a 500.
- BR-4 stripping stays in `Calculation.result()`, unchanged.

*Alternative:* catch `ArithmeticException` around `divide`. Rejected, because it relies on JDK message text to tell the cases apart and hides the rule.

### D2. `DivisionByZeroException` is a plain domain `RuntimeException`

It mirrors `UnknownOperationException`: it lives in `domain`, has no framework imports, and carries no fields (the divisor is always zero). ArchUnit's domain allow-list already covers it.

### D3. One new handler in `ProblemDetailsAdvice`

`@ExceptionHandler(DivisionByZeroException.class)` returns `ProblemDetail` with status `422`, type `/problems/division-by-zero`, title "Division by zero", and detail "Cannot divide by zero." Use `HttpStatus.UNPROCESSABLE_CONTENT` (Spring 7's name for 422). Fall back to `HttpStatus.valueOf(422)` if the enum constant name differs. The existing shape test helper (`expectProblem`) is reused, so 422 gets the same `type/title/status/detail` and no-internals checks.

### D4. Tests

- `OperationTest`: add DIVIDE rows to the parameterised table (the spec's six scenarios). Add `toPlainString`/scale assertions for the tie cases, because `isEqualByComparingTo` alone wouldn't catch HALF_UP vs HALF_EVEN on `…0005`. Add a parameterised test that each zero representation, including `0 ÷ 0`, throws `DivisionByZeroException`.
- `CalculationControllerTest`: swap `DIVIDE` → `MODULO` in `rejectsUnknownOperation`. Add `DIVIDE` happy-path rows (`1 ÷ 3`, `1 ÷ 4`). Add a parameterised 422 test over the zero forms (strings plus JSON number `0.0`) using `expectProblem`.
- The smoke test needs no change.

## Risks / Trade-offs

- [The tie-case scenarios depend on input precision exceeding 34 digits] → They also verify that operands with 35 significant digits are *accepted* today. When BR-1 (≤ 100 digits) lands in item 4 they stay valid, since 35 < 100.
- [Clients that relied on `DIVIDE` → 400 unknown-operation] → No known clients. Adding an operation is non-breaking per PRD §3.4.
- [`HttpStatus` 422 constant renamed across Spring versions] → D3's fallback. The compiler catches it immediately.

## Open Questions

None.
