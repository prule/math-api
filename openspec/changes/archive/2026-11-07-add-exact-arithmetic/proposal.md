# Proposal

## Why

The walking skeleton has no behaviour yet. This change delivers the product's core: a client can POST two operands and get an exact `ADD`, `SUBTRACT` or `MULTIPLY` Result back. It also builds the shared request/response contract and Problem Details error handling that every later operation reuses. It's backlog item 2, the first change that touches every layer.

## What Changes

- New endpoint `POST /api/v1/calculations` that takes `{ operation, a, b }` and returns `{ operation, a, b, result }` with every numeric value as a string (PRD §3.2).
- Operations `ADD`, `SUBTRACT`, `MULTIPLY`, all exact (BR-3), with every Result normalised per BR-4.
- Operands are accepted as JSON numbers or JSON strings.
- RFC 9457 Problem Details for `400 /problems/invalid-request` (malformed JSON, missing field, non-numeric operand) and `400 /problems/unknown-operation`.
- The existing smoke test gains one happy-path calculation.
- The ArchUnit "empty layers allowed" shortcut is removed, since every layer now has a class.

**Out of scope:** `DIVIDE` with BR-2/BR-3 rounding (backlog item 3). Until item 3 lands, a `DIVIDE` request gets the same `unknown-operation` response as any other unsupported name. Also out of scope: operand limits BR-1 (item 4), OpenAPI/springdoc (item 5), case-insensitive operation names, and any new operation beyond these three.

## Capabilities

### New Capabilities

- `calculation`: performing one Operation on two Operands over HTTP. Covers the request/response contract, the supported operations and their exactness, Result normalisation, and the error responses for invalid requests and unknown operations. Later operations and rules (DIVIDE, BR-1) will modify this capability.

### Modified Capabilities

None. `service-health` doesn't change.

## Impact

- **Code:**
  - `domain`: `Operation`, `Calculation`, `UnknownOperationException`.
  - `application`: `PerformCalculation`.
  - `adapter.in.web`: controller, request/response records, a single `@RestControllerAdvice`.
  - `config`: use-case wiring.
  - Tests in all four layers, plus `ArchitectureTest` with the shortcut removed.
- **API:** adds `POST /api/v1/calculations`. Non-breaking.
- **Dependencies:** none new. It uses the existing Spring Boot webmvc and validation starters and Jackson 3.
- **Docs:**
  - `docs/PRD.md`: §3.2 states that operation names are exact and case-sensitive, and that `a`/`b` are echoed in plain notation. Add a change log row.
  - `docs/DOMAIN-MODEL.md`: §2 lists `UnknownOperationException` among the domain failures and states that Operation names are exact. Add a change log row.
  - `docs/STANDARDS.md`: none.
  - `docs/BACKLOG.md`: set item 2 to Done.
