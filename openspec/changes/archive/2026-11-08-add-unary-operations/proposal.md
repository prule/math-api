# Proposal

## Why

PRD §6 lists unary operations (sqrt, negate) as the next roadmap item, and it's backlog item 7. Today every Operation takes two Operands and `b` is mandatory, so a one-Operand Operation cannot be expressed. The domain has to model arity, and the request shape has to allow `b` to be left out without breaking existing clients.

## What Changes

- **Arity:** every Operation is either unary (one Operand, `a`) or binary (`a` and `b`). The six existing Operations are binary (new BR-9).
- **Request shape:** `b` stays required for binary Operations, as today. For a unary Operation, `b` MUST be omitted (or `null`). Sending `b` to a unary Operation → **400** `/problems/invalid-request` whose detail names `b` (BR-9).
- **Response shape:** for a unary Operation the body is `{ operation, a, result }`. The `b` key is left out, not set to `null`. Binary responses don't change.
- **NEGATE** (`−a`): exact (BR-3). Zero in any representation gives `"0"` (BR-4).
- **SQRT** (`√a`): correctly rounded to 34 significant digits, HALF_EVEN. BR-3 is widened to cover it, so `√4` stays `"2"` and `√2` is `"1.414213562373095048801688724209698"`. A negative `a` → **422** `/problems/negative-square-root` (new BR-8). The square root of zero in any representation is `"0"`.
- **Validation order:** a `b` that doesn't match the arity is checked right after the unknown-operation check. The square-root rule comes after division by zero.
- **OpenAPI:** `b` is no longer listed as required. Its description and the response's `b` description say when it's present. The 422 response gains a `negative-square-root` example. The allowed operation names and the unknown-operation example list eight names.
- **Non-breaking** per PRD §3.4. Every request that succeeds today still succeeds with the same body. Every request that fails today still fails with the same status and `type`, with one exception: a request with an unknown `operation` *and* no `b` used to get `invalid-request` and now gets `unknown-operation`. Both are 400. The `detail` text for a missing `b` stays human-readable but may be worded differently.

**Out of scope:** other unary operations (abs, ln, …), n-ary operations, an operations-discovery endpoint that reports arity (backlog item 8), client-chosen precision (item 9), and a SQRT speed test (BR-1 already bounds the Operand; see design.md).

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calculation`:
  - **Added:** "Operand count matches the Operation" (BR-9), "NEGATE is exact", "SQRT rounds to 34 significant digits" and "Square root of a negative rejected" (BR-8).
  - **Modified:** "Perform a Calculation" (`b` optional for unary, left out of the response), "Invalid request rejected" (`b` is required only for binary Operations) and "Validation order" (arity and BR-8).
- `api-documentation`:
  - **Modified:** "Operands documented as number or string" (only `operation` and `a` are required), "Allowed operations documented from the supported set" (eight names), "Error responses documented" (adds `negative-square-root`) and "Unknown-operation example lists the supported operations" (eight names).

## Impact

- **Code:**
  - `domain.Operation`: an arity per constant, plus `SQRT` and `NEGATE`.
  - `domain.Calculation`: `b` is nullable and checked against the arity before BR-1.
  - New domain exceptions `ArityMismatchException` and `NegativeSquareRootException`.
  - `adapter.in.web`: `CalculationRequest.b` is no longer `@NotNull`. `CalculationResponse.b` is left out when null. `ProblemDetailsAdvice` maps the two new exceptions. `ProblemExamples` gains the new example and lists eight operations.
  - `config.OpenApiConfig`: the description no longer says "two operands".
- **Docs:**
  - `docs/PRD.md`: §1, §3.1, §3.2 (request/response shape, error table, validation order), §6 roadmap and the change log.
  - `docs/DOMAIN-MODEL.md`: glossary (Operation, Calculation and the new Arity term), §2 model, §3 operations, BR-3 widened, new BR-8 and BR-9, and the change log.
  - `docs/STANDARDS.md`: the "adding an operation" rule now includes declaring the arity, plus the change log.
- **Clients:** existing clients are unaffected (see What Changes).
