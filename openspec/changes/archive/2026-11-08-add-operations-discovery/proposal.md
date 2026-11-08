# Proposal

## Why

The service now has eight Operations, two of them unary, and a client can only learn which exist and how many Operands each takes from the docs or from an `unknown-operation` error. PRD §6 notes discovery is "useful once the operation list grows"; it has grown, and it's backlog item 8.

## What Changes

- **New endpoint** `GET /api/v1/operations` → **200** `application/json`: an array with one entry per supported Operation, in the order the service declares them (the same order as the `unknown-operation` detail):

  ```json
  [ { "name": "ADD", "arity": 2 }, …, { "name": "SQRT", "arity": 1 }, { "name": "NEGATE", "arity": 1 } ]
  ```

  `name` is the exact, case-sensitive value to send as `operation`; `arity` is a JSON integer, 1 or 2 (BR-9).
- **Derived from the domain:** the list is built from the Operation set itself, so adding an Operation changes the response with no edit to the endpoint (STANDARDS §3 "nothing else should need to change" still holds).
- **No error responses of its own:** the endpoint takes no input. Other methods on the path get the existing framework 405 Problem Details.
- **OpenAPI:** the endpoint is documented with its 200 `application/json` response, an array of objects with a string `name` and an integer `arity`.
- **Non-breaking** per PRD §3.4: a new path; `POST /api/v1/calculations` is unchanged.

**Out of scope:** per-Operation descriptions, rounding or rule metadata (BR-3, BR-5…); `GET /api/v1/operations/{name}`; HATEOAS links (PRD §5 non-goal); caching headers; any change to the calculation contract.

**Docs to update:**
- `PRD.md`: §3.2 gains the endpoint; §5 drops the discovery non-goal; §6 marks the roadmap row delivered; change log.
- `STANDARDS.md`: §4 "numeric values in responses are JSON strings" is clarified to mean decimal values (Operands, Results); counts such as arity are JSON integers; change log.
- `DOMAIN-MODEL.md`: none (no new term or rule; Arity and BR-9 already exist).
- `README.md`: one line pointing at the endpoint next to the existing "supported operations" pointer.

## Capabilities

### New Capabilities

- `operation-discovery`: lets a client list the supported Operations with their Arity over HTTP.

### Modified Capabilities

- `api-documentation`:
  - **Added:** "Operations endpoint documented" (path, 200 `application/json`, `name` string and `arity` integer, only a 200 response).

## Impact

- **Code:** new `adapter.in.web.OperationController` and `OperationResponse` record. No domain, application or config change.
- **Tests:** new web-slice `OperationControllerTest` (every Operation listed, in order, with its arity); `ApiDocumentationTest` gains the new path's assertions.
- **API:** one new read-only path. No dependency changes.
