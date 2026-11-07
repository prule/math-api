# Math API — Backlog

> Living document. Each item is one OpenSpec change. Work top to bottom.
> When you start an item, set it to **In progress**; when you `/opsx:archive` it, set it to **Done** and note the change name.
> Source of scope: [PRD](PRD.md). Rules: [Domain Model](DOMAIN-MODEL.md). How: [Standards](STANDARDS.md).

## Status

| # | Item | Status | OpenSpec change |
|---|------|--------|-----------------|
| 1 | Walking skeleton | Done | `add-walking-skeleton` |
| 2 | Exact operations: add, subtract, multiply | Todo | |
| 3 | Divide | Todo | |
| 4 | Operand limits | Todo | |
| 5 | OpenAPI documentation | Todo | |

Items 1–5 complete PRD v1. Items under **Later** need a PRD update first (each one moves something out of the non-goals or roadmap).

---

## v1

### 1. Walking skeleton

No business behaviour; proves the build, layers and CI work so every later change is just behaviour.

```text
/opsx:propose Walking skeleton for math-api per docs/STANDARDS.md: Gradle Kotlin DSL wrapper, Java 25, Spring Boot 4 (web, validation, actuator), base package com.paulrule.mathapi with empty domain / application / adapter.in.web / config packages. Add Spotless with palantir-java-format, ArchUnit tests enforcing the STANDARDS.md §2 dependency rules, one @SpringBootTest smoke test that the context starts and GET /actuator/health returns UP, a GitHub Actions workflow running ./gradlew build on PRs and pushes to main, and a .gitignore (Gradle, IDE, logs/). Out of scope: any calculation behaviour, OpenAPI docs.
```

Done when: `./gradlew build` passes locally and in CI; ArchUnit fails if `domain` imports Spring.

### 2. Exact operations: add, subtract, multiply

First vertical slice through every layer, plus the shared request/response and error handling.

```text
/opsx:propose Add the Calculation capability with ADD, SUBTRACT and MULTIPLY per docs/PRD.md §3 and docs/DOMAIN-MODEL.md. Domain: Operation enum (each constant implements apply), Calculation record, results normalised per BR-4 (trailing zeros stripped, plain notation). Application: a PerformCalculation use case wired in config. Web: POST /api/v1/calculations accepting operands as JSON numbers or strings and returning operation, a, b, result as strings; a single @RestControllerAdvice returning RFC 9457 Problem Details for /problems/invalid-request and /problems/unknown-operation (400). Test-first: domain unit tests for every edge case in DOMAIN-MODEL.md §3 for these three operations (incl. 0.1 + 0.2 = 0.3), web-slice tests for every error path, extend the smoke test with one happy-path request. Once every layer has a class, remove the allowEmptyShould / withOptionalLayers shortcut in ArchitectureTest. Out of scope: DIVIDE, operand limits (BR-1), OpenAPI.
```

Done when: `0.1 + 0.2` returns `"0.3"`; unknown operation and malformed body return the PRD's Problem Details.

### 3. Divide

Adds the only rounding rule and the only 422.

```text
/opsx:propose Add DIVIDE to the Calculation capability per docs/DOMAIN-MODEL.md BR-2 and BR-3: MathContext.DECIMAL128 (34 significant digits, HALF_EVEN), result normalised per BR-4. Division by any zero representation (0, 0.00, -0) raises a domain DivisionByZeroException mapped to 422 Problem Details with type /problems/division-by-zero. Test-first: 1 ÷ 3 rounding, exact 1 ÷ 4 = 0.25, negative divisor, each zero representation; web-slice test for the 422. Should only need a new Operation constant, an exception and its mapping — flag in design.md if anything else has to change.
```

Done when: `1 ÷ 3` returns 34 significant digits; `x ÷ 0` returns 422.

### 4. Operand limits

Closes the CPU-exhaustion hole before anyone else calls the API.

```text
/opsx:propose Enforce docs/DOMAIN-MODEL.md BR-1 on every Operand: precision() ≤ 100 and -1000 ≤ scale() ≤ 1000, checked in the domain (OperandOutOfRangeException) and mapped to 400 Problem Details with type /problems/operand-out-of-range. Test-first: values exactly at each limit are accepted, one past each limit is rejected, 1e999999999 is rejected quickly (before any arithmetic). Also confirm JSON parsing of huge numbers/strings cannot itself exhaust CPU or memory (Jackson stream read constraints); set a sensible request-size limit if needed. Out of scope: making the limits configurable.
```

Done when: boundary tests pass; `1e999999999 × 1e999999999` returns 400 immediately.

### 5. OpenAPI documentation

```text
/opsx:propose Add springdoc-openapi (the release compatible with Spring Boot 4) serving /v3/api-docs and /swagger-ui.html, per docs/PRD.md §3.3. Document POST /api/v1/calculations: the request schema (operands as number or string), the response schema (all strings), the allowed operation values, and every Problem Details response (400 types and 422) with examples. Add a test asserting /v3/api-docs contains the calculations path and its error responses. Out of scope: hand-written docs outside the generated spec.
```

Done when: Swagger UI shows the endpoint with all four operations and every error response.

---

## Later (post-v1 — update PRD.md first)

Each prompt below starts by moving the item from PRD §5/§6 into scope. Order is a suggestion; reorder as needs arrive.

### 6. More binary operations (power, modulo)

```text
/opsx:propose Add POWER and MODULO operations. First update docs/PRD.md (§3.1, roadmap, change log) and add their business rules to docs/DOMAIN-MODEL.md with new BR ids: POWER exponent must be an integer within a bounded range (prevent CPU exhaustion), negative/fractional exponent behaviour, MODULO by zero, sign of MODULO result for negative operands. Then implement as new Operation constants, test-first.
```

### 7. Unary operations (sqrt, negate)

```text
/opsx:propose Add unary operations SQRT and NEGATE. Design decision needed in design.md: how the domain expresses arity (enum with arity vs. separate types) and the request shape (b optional vs. rejected when present) — must stay non-breaking for existing clients of /api/v1/calculations. Update PRD.md, DOMAIN-MODEL.md (glossary, new BR ids incl. sqrt of a negative and sqrt precision) and STANDARDS.md if the "adding an operation" rule changes.
```

### 8. Operations discovery endpoint

```text
/opsx:propose Add GET /api/v1/operations listing the supported operations (name and arity), derived from the Operation enum so it can never drift. Update PRD.md (move out of non-goals, add to §3.2). Web-slice test asserting every Operation constant is listed.
```

### 9. Client-chosen precision

```text
/opsx:propose Add an optional precision (significant digits) and roundingMode to the calculation request, defaulting to BR-3 behaviour so existing clients are unaffected. Bound precision to prevent CPU exhaustion. Update PRD.md, DOMAIN-MODEL.md (amend BR-3 or add a new BR), test-first.
```

### 10. Calculation history

```text
/opsx:propose Persist each successful Calculation and add GET /api/v1/calculations (paged) and GET /api/v1/calculations/{id}. Add a repository port in application and an adapter.out.persistence adapter; choose the store in design.md. This ends statelessness — update PRD.md (§4, §5), DOMAIN-MODEL.md (Calculation gains identity and timestamp) and STANDARDS.md (persistence and migration conventions).
```

### 11. Authentication and rate limiting

```text
/opsx:propose Add authentication (choose mechanism in design.md: API key vs OAuth2 resource server) and per-client rate limiting returning 429 Problem Details. Adapter-level only — the domain must not change (ArchUnit proves it). Update PRD.md (§2 users, §5 non-goals) and STANDARDS.md (security conventions).
```
