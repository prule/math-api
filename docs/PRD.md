# Math API — Product Requirements

> Living document. Update it in the same change as any change to scope, API contract or operations (see `CLAUDE.md`).
> Related: [Domain Model](DOMAIN-MODEL.md) · [Standards](STANDARDS.md)

## 1. Problem and goal

Clients need exact, predictable arithmetic over HTTP without each one re-implementing decimal handling.
Math API is a stateless REST service that applies one arithmetic operation to two operands and returns an exact decimal result.

## 2. Users

Internal services and developers (portfolio / learning-grade). No public-internet audience in v1.

## 3. Scope (v1)

### 3.1 Operations

| Operation  | Meaning | Notes |
|------------|---------|-------|
| `ADD`      | a + b   | |
| `SUBTRACT` | a − b   | |
| `MULTIPLY` | a × b   | |
| `DIVIDE`   | a ÷ b   | Rounded to 34 significant digits (BR-3); b = 0 rejected (BR-2) |

All operations are binary. Semantics and rules live in [DOMAIN-MODEL.md](DOMAIN-MODEL.md).

### 3.2 API contract

`POST /api/v1/calculations`

Request — operands may be JSON numbers or strings:

```json
{ "operation": "DIVIDE", "a": 1, "b": "3" }
```

`200 OK` — all numeric values returned as strings to avoid client-side precision loss:

```json
{ "operation": "DIVIDE", "a": "1", "b": "3", "result": "0.3333333333333333333333333333333333" }
```

- `operation` must be exactly a supported name (case-sensitive: `ADD`, not `add`).
- `a` and `b` are echoed as received, in plain notation and with their given scale (`1e2` → `"100"`, `"1.50"` → `"1.50"`). Only `result` is normalised (BR-4).
- Every error response (including framework errors such as 415) is Problem Details with `type`, `title`, `status` and a human-readable `detail` that exposes no internals.

Errors use RFC 9457 Problem Details (`application/problem+json`):

| Status | When | `type` |
|--------|------|--------|
| 400 | Malformed JSON, missing field, non-numeric operand | `/problems/invalid-request` |
| 400 | Unknown `operation` | `/problems/unknown-operation` |
| 400 | Operand outside limits (BR-1) | `/problems/operand-out-of-range` |
| 422 | Division by zero (BR-2) | `/problems/division-by-zero` |

### 3.3 Supporting endpoints

- `GET /actuator/health` — liveness/readiness.
- OpenAPI docs via springdoc: `/v3/api-docs`, `/swagger-ui.html`.

### 3.4 Versioning

Path-based (`/api/v1`). Adding an operation is non-breaking and stays in v1. Any breaking contract change ships as `/api/v2`.

## 4. Non-functional requirements

- **Exactness:** `BigDecimal` throughout; `0.1 + 0.2` returns `"0.3"`.
- **Safety:** operand limits (BR-1) prevent CPU-exhaustion via huge exponents.
- **Stateless:** no database, no session; any instance can serve any request.
- **Stack:** Java 25, Spring Boot 4 — see [STANDARDS.md](STANDARDS.md).

## 5. Non-goals (v1)

- Authentication, authorization, rate limiting.
- Persistence / calculation history.
- Expression parsing (`"2 + 3 * 4"`) or n-ary operations (`sum[1,2,3]`).
- Client-chosen precision or rounding mode.
- An operations-discovery endpoint (`GET /operations`).
- HATEOAS links.

## 6. Roadmap / known future changes

| Item | Impact when it lands |
|------|---------------------|
| More binary operators (power, modulo, …) | New `Operation` constant + rules + tests; non-breaking |
| Unary operations (sqrt, negate, …) | Domain model must express arity; request shape may need `b` optional — decide in its OpenSpec change |
| Client-chosen precision | New optional request field; non-breaking |
| Calculation history | Adds a repository port + persistence adapter |
| Auth / rate limiting | New adapter-level concern |
| `GET /operations` discovery | Useful once the operation list grows |

## 7. Success criteria (v1)

- All four operations return correct exact results, including the edge cases listed in the domain model.
- Every error path in §3.2 returns the stated status and Problem Details `type`.
- `./gradlew build` passes in CI (tests, ArchUnit, Spotless).

## 8. Change log

| Date | Change |
|------|--------|
| 2026-11-07 | Initial PRD: v1 scope with four basic operations |
| 2026-11-07 | `add-exact-arithmetic`: §3.2 clarifies case-sensitive operation names, operand echo format, Problem Details for all errors |
