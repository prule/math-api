# Proposal

## Why

PRD §6 lists "more binary operators (power, modulo)" as the first post-v1 roadmap item, and it's backlog item 6, which opens v2. Both are common requests, but neither is a one-line enum constant. POWER can produce results with a million digits and needs an exponent rule. MODULO's sign for negative operands differs between languages, so it has to be stated.

## What Changes

- **POWER** (`a^b`):
  - `b` must be an integer with |b| ≤ 999 (BR-5). A fractional or larger `b` → **422** `/problems/invalid-exponent`.
  - Negative `b` means `1 / a^|b|`. Zero to a negative power → 422 `/problems/division-by-zero` (BR-2). `0^0 = 1`.
  - The Result is the exact power rounded once to 34 significant digits, HALF_EVEN (BR-3), so `2^10` stays `"1024"`.
  - A rounded Result whose scale is outside ±1000 → **422** `/problems/result-out-of-range` (BR-6).
- **MODULO** (`a mod b`): truncated remainder, so the sign follows `a` (`−7 mod 3 = −1`, `5.5 mod 2 = 1.5`). It's exact (BR-7), and a zero `b` → 422 `/problems/division-by-zero` (BR-2).
- The validation order gains these rules, the unknown-operation example uses a name that will never be an Operation, and the documented operation list grows to six.
- **OpenAPI:** the 422 response documents the two new types. A new test checks that the unknown-operation example's list equals the supported Operations (from the v2 audit).
- **Non-breaking** per PRD §3.4: two new operation names, and two new 422 `type`s that only those operations can return.

**Out of scope:** fractional or real-number exponents, floored or Euclidean modulo, integer-only operations, client-chosen precision (backlog item 9), unary operations (item 7), and result limits for the existing four operations.

## Capabilities

### New Capabilities

None.

### Modified Capabilities

- `calculation`:
  - **Added:** POWER and MODULO requirements.
  - **Modified:** "Division by zero rejected" (covers MODULO and zero to a negative power), "Unknown Operation rejected" (the scenario name stops using POWER), and "Validation order" (exponent and result rules).
- `api-documentation`:
  - **Modified:** "Allowed operations documented from the supported set" now lists six names.
  - **Modified:** "Error responses documented" adds the two 422 types.
  - **Added:** the unknown-operation example must list exactly the supported Operations.

## Impact

- **Code:**
  - `domain.Operation`: `POWER`, `MODULO`.
  - New `domain.InvalidExponentException` and `domain.ResultOutOfRangeException`.
  - `ProblemDetailsAdvice`: two handlers.
  - `ProblemExamples` and the controller's 422 `@ApiResponse`: two new examples, and the operation list updated.
  - Tests: `OperationTest`, `CalculationControllerTest`, `ApiDocumentationTest`.
- **API:** new operations `POWER` and `MODULO`; new 422 types `/problems/invalid-exponent` and `/problems/result-out-of-range`. Non-breaking.
- **Dependencies:** none.
- **Docs:**
  - `docs/PRD.md`: §3.1 operation rows, §3.2 error table, §3.2 validation order, §6 roadmap row done, change log.
  - `docs/DOMAIN-MODEL.md`: model diagram, §2 exceptions, §3 rows, BR-2 and BR-3 amended, new BR-5 to BR-7, change log.
  - `docs/BACKLOG.md`: item 6 status.
  - `docs/STANDARDS.md`: none.
