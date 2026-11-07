# Proposal

## Why

Any Operand is accepted today, however large. Inputs like `1e-999999999 ÷ 1e999999999` overflow `BigDecimal`'s scale and return a 500, and `ADD` of extreme exponents can exhaust memory. Jackson's defaults also accept string tokens up to 100,000,000 characters with no document-size limit, so a single request can burn CPU parsing a huge digit string into a `BigDecimal`. BR-1 has been written down since the start but never enforced. It's backlog item 4, and the API must not be deployed publicly until it lands.

## What Changes

- Every Operand is checked against BR-1 **as written**: at most 100 significant digits (`precision() ≤ 100`) and `-1000 ≤ scale() ≤ 1000`. This happens in the domain, before any arithmetic.
- A violating Operand is rejected with **400** Problem Details, type `/problems/operand-out-of-range`, and a detail naming the operand (`a` or `b`) and the limit it broke.
- JSON read limits bound the work done before the domain sees the request:
  - request document ≤ 16,384 characters
  - any string token ≤ 1,000 characters
  - number tokens ≤ 1,000 characters (the existing default, now stated explicitly)
  - Requests over these limits are rejected with `400 /problems/invalid-request`.
- Order of checks: unknown operation → BR-1 → BR-2. For example, `DIVIDE` with an out-of-range divisor of zero returns 400 operand-out-of-range, not 422.
- BR-1's explanatory note in DOMAIN-MODEL.md is corrected. The limit applies to the operand as written, so `1E+1000` is accepted, `1E+1001` is rejected, and a 101-digit integer such as `1` followed by 100 zeros is rejected (write `1E+100` instead).

**Out of scope:** configurable limits, limits on *Results* (they're bounded by the operand limits), HTTP-level request-size limits outside JSON parsing, and rate limiting.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calculation`: adds requirements for operand limits (BR-1), bounded request parsing, and the order in which validation rules apply. No existing requirement text changes: oversized requests reuse the existing `invalid-request` type.

## Impact

- **Code:**
  - `domain`: new `OperandOutOfRangeException`. `Calculation`'s compact constructor checks BR-1 for `a` and `b`.
  - `adapter.in.web`: `ProblemDetailsAdvice` gains one handler.
  - `src/main/resources/application.yaml`: Jackson read constraints.
  - Tests: `CalculationTest`, `CalculationControllerTest`.
- **API:** new `400 /problems/operand-out-of-range`. Oversized requests now return `400 invalid-request` instead of being parsed. Operands that used to be accepted beyond the limits are now rejected. This is a tightening the PRD has always specified, and no known clients rely on the old behaviour.
- **Dependencies:** none. It uses Spring Boot 4.1's `spring.jackson.factory.constraints.read.*` properties.
- **Docs:**
  - `docs/DOMAIN-MODEL.md`: correct BR-1's note, list `OperandOutOfRangeException` as implemented, add a change log row.
  - `docs/PRD.md`: add a §3.2 note on request-size limits and the order of checks, plus a change log row.
  - `docs/BACKLOG.md`: set item 4 to Done.
  - `docs/STANDARDS.md`: none.
