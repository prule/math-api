# Proposal

## Why

`DIVIDE` is the last of the four v1 operations in PRD §3.1, and the only one that can't be exact. It needs a fixed rounding rule (BR-3) and the API's only domain-rule rejection, division by zero (BR-2, HTTP 422). It's backlog item 3, and the first test of the claim that adding an Operation is a one-place change.

## What Changes

- `DIVIDE` becomes a supported Operation on `POST /api/v1/calculations`.
- Its Result is computed to 34 significant digits with HALF_EVEN rounding (BR-3), then normalised per BR-4 (`1 ÷ 4` → `"0.25"`, `1 ÷ 3` → `"0.3333333333333333333333333333333333"`).
- Dividing by any zero Operand (`0`, `0.00`, `-0`, `0E+5`) is rejected with **HTTP 422** Problem Details, type `/problems/division-by-zero` (BR-2).
- The unknown-operation detail message now lists `DIVIDE` among the supported operations. This follows automatically from the enum.
- **Behaviour change for clients:** `DIVIDE` used to return `400 /problems/unknown-operation`. It now returns 200 or 422. This is non-breaking per PRD §3.4, since adding an operation stays in v1.

**Out of scope:** operand limits BR-1 (item 4), client-chosen precision or rounding (PRD roadmap), integer/modulo division, and any other new Operation.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calculation`: adds requirements for `DIVIDE` rounding (BR-3) and division-by-zero rejection (BR-2). Existing requirements are unchanged, including "Unknown Operation rejected", whose scenarios don't mention `DIVIDE`.

## Impact

- **Code:** `domain.Operation` gains `DIVIDE`; new `domain.DivisionByZeroException`; `adapter.in.web.ProblemDetailsAdvice` gains one handler. Tests: `OperationTest`, `CalculationControllerTest`. That one web test currently uses `DIVIDE` as its example of an *unknown* operation and must swap it for another name.
- **API:** `DIVIDE` now supported; new `422` response. Non-breaking.
- **Dependencies:** none.
- **Docs:**
  - `docs/DOMAIN-MODEL.md`: change log row (DIVIDE implemented; `DivisionByZeroException` no longer "later").
  - `docs/BACKLOG.md`: set item 3 to Done.
  - `docs/PRD.md`: none. §3.1 and §3.2 already describe DIVIDE and the 422.
  - `docs/STANDARDS.md`: none.
