# Design

## Context

See proposal.md (Why). Current state:

- `Operation` is an enum, and each constant implements `apply(BigDecimal a, BigDecimal b)`. `Operation.named` is the lookup. `OpenApiConfig` and `ProblemDetailsAdvice` both list `Operation.values()`.
- `Calculation(operation, a, b)` rejects a null `b` (`Objects.requireNonNull`) and checks BR-1 on both Operands.
- `CalculationRequest.b` is `@NotNull`. A missing `b` fails Bean Validation in the web adapter (`MethodArgumentNotValidException` → `invalid-request`, "Missing required field(s): 'b'."), before `Operation.named` runs.
- `CalculationController` echoes `calculation.b().toPlainString()`.

## Goals / Non-Goals

**Goals:**
- Express arity in the domain with the least change to the existing seams (enum, lookup, OpenAPI generation, advice).
- Keep every existing binary request and response byte-for-byte unchanged.

**Non-Goals:**
- Supporting arity above 2, or a generic Operand list.
- Exposing arity in the API. That belongs to backlog item 8 (`GET /operations`).

## Decisions

### 1. Arity is a field on the `Operation` enum (user decision)

Each constant passes its arity to the enum constructor (`ADD(2) { … }`, `SQRT(1) { … }`), and `Operation.arity()` returns it. `apply(a, b)` keeps its signature. Unary constants ignore `b`, which is always `null` for them.

- **Why:** `Operation.named`, the generated OpenAPI enum, the unknown-operation list and every existing test keep working unchanged. An `int` matches the "name and arity" that item 8 will report.
- **Alternative: a sealed `UnaryOperation` / `BinaryOperation` split.** More type-safe, but it rewrites the lookup, the OpenAPI customizer, the advice and every caller for no behavioural gain. Rejected by the user.
- **Alternative: an `apply(a)` overload for unary.** It adds a second abstract method that every constant must answer. Ignoring `b` is one line per unary constant.

### 2. `b` given to a unary Operation is rejected; `null` counts as omitted (user decision)

The arity rule (BR-9) is checked in the `Calculation` constructor, before BR-1: `(b == null) != (operation.arity() == 1)` → `ArityMismatchException(operation)`. The web adapter drops `@NotNull` from `CalculationRequest.b`, so a missing `b` now reaches the domain. A missing `b` and an explicit `null` are the same thing after Jackson binds them, which is why `"b": null` counts as omitted.

- **Why in the domain:** arity is a business rule that depends on the Operation, so it can't be checked until the Operation is known. Keeping it with BR-1 puts all Operand rules in one place.
- **Mapping:** `ProblemDetailsAdvice` maps `ArityMismatchException` to 400 `/problems/invalid-request`. The detail comes from the arity: a binary Operation keeps today's "Missing required field(s): 'b'.", and a unary one says "'b' must be omitted for SQRT, which takes one operand.". No new Problem type is added: a wrong field set is an invalid request.
- **Alternative: ignore `b` for unary.** It hides client bugs, and the echo would be ambiguous. Rejected by the user.

**Observable consequence:** a request with an unknown `operation` and no `b` now gets `unknown-operation` instead of `invalid-request`, because the lookup runs before the arity check. Both are 400. This is recorded in the proposal and the validation-order spec.

### 3. Response leaves `b` out for unary Operations

`CalculationResponse.b` gets `@JsonInclude(NON_NULL)` (annotation from `com.fasterxml.jackson.annotation`, which Jackson 3 still uses). The controller passes `null` when `calculation.b()` is null. Binary responses are unchanged.

- **Alternative: `"b": null`.** It adds a key that carries no information, and strict clients that expect a string would choke on it.

### 4. SQRT precision widens BR-3; the negative case is a new BR-8

`SQRT` is `a.sqrt(MathContext.DECIMAL128)`, guarded by `a.signum() < 0 → NegativeSquareRootException` (otherwise `BigDecimal.sqrt` throws `ArithmeticException`). The backlog prompt asks for a new BR id for SQRT precision. Instead, BR-3, which is already "the rounding rule" for DIVIDE and POWER, gains SQRT, the same way `add-power-modulo` widened it for POWER. A separate rule would state the same 34-digit HALF_EVEN policy twice. The new BR ids are BR-8 (square root of a negative is rejected) and BR-9 (arity).

- **Correct rounding:** checked on the project's JDK 25: `BigDecimal.sqrt(DECIMAL128)` rounds exact ties to even and is exact when the root fits in 34 digits. The spec pins this with "Ties round to even" (which fails under HALF_UP, the default of `new MathContext(34)`), "Last digit rounds up" (`√10`) and the exact roots (`√0.25`).
- **Zero:** `signum()` is 0 for every zero form, so `√0`, `√-0` and `√0.00` give `0`.
- **Result bound:** for any BR-1 Operand, the root's scale is about half the Operand's (`√1E+1000 = 1E+500`, `√1E-1000 = 1E-500`), so it can't leave ±1000. BR-6 doesn't need to cover SQRT.

### 5. NEGATE

`NEGATE` is `a.negate()`, exact. BigDecimal has no negative zero, and BR-4 normalisation gives `"0"`.

### 6. New Problem type `/problems/negative-square-root` (422)

This follows the existing 422 pattern: a domain exception (`NegativeSquareRootException`), an advice handler, a `ProblemExamples.NEGATIVE_SQUARE_ROOT` example and the controller's 422 `@ApiResponse` example. Its detail is "SQRT needs a non-negative operand.". It doesn't echo the value, matching the BR-1 detail policy.

## Risks / Trade-offs

- [SQRT on a 100-digit Operand at scale ±1000 is slow] → Measured on JDK 25: 2,000 roots of the worst BR-1 Operands (one hundred 9s at scale −900 and 1000) take 13 ms in total. `BigDecimal.sqrt` works at the target precision whatever the Operand's scale, and BR-1 caps the Operand at 100 digits. There's no speed test; the `1E+1000` scenario exercises the extreme scale. Add a timing test only if a profile ever shows otherwise.
- [A client relied on the `invalid-request` type for "unknown operation and missing `b`"] → Both are 400, and the case is a doubly broken request. It's noted in the PRD change log.
- [`b` is no longer `required` in the OpenAPI schema, so a generated client might stop sending it for binary Operations] → The `b` description states the rule, and the server still rejects it with 400.
