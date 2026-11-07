# Proposal

## Why

The calculation API's contract (operands as number or string, all-string responses, case-sensitive operation names, four Problem Details error types) currently exists only in the PRD and specs. Clients need a machine-readable description they can browse and generate code from. PRD §3.3 promised OpenAPI docs in v1, and this is backlog item 5, the last v1 item.

## What Changes

- New dependency: springdoc-openapi **3.2.0** (`springdoc-openapi-starter-webmvc-ui`), the release built against Spring Boot 4.1.2.
- `GET /v3/api-docs` serves an OpenAPI **3.1** document. `GET /swagger-ui.html` serves Swagger UI.
- `POST /api/v1/calculations` is documented with:
  - the request schema: `operation` with its allowed values taken from the `Operation` enum, so adding an Operation updates the docs with no edits; `a`/`b` accepted as a number or a string
  - the response schema, with every field a string
  - every error response as `application/problem+json`, with one named example per `type`: 400 `invalid-request`, `unknown-operation`, `operand-out-of-range`; 422 `division-by-zero`
- A test asserts the generated document contains the path, the allowed operations, the operand number-or-string type, and each error response and example.

**Out of scope:** hand-written docs outside the generated spec, documenting actuator endpoints, authentication on the docs endpoints (the API has none in v1), API-docs versioning or publishing, and client SDK generation.

## Capabilities

### New Capabilities

- `api-documentation`: the machine-readable OpenAPI description and the interactive UI for the HTTP API, and the rule that the description stays in step with the calculation contract.

### Modified Capabilities

None. `calculation` behaviour doesn't change, and `service-health`'s "no other operational endpoints" rule covers actuator endpoints only, which springdoc doesn't expose.

## Impact

- **Code:**
  - `build.gradle.kts`: the springdoc dependency.
  - `adapter.in.web`: OpenAPI annotations on `CalculationController`, `CalculationRequest`, `CalculationResponse`.
  - `config`: an `OpenApiConfig` with API info, plus a customiser that fills `operation`'s allowed values from `Operation.values()`.
  - `application.yaml`: springdoc paths and the OpenAPI version.
  - New test `ApiDocumentationTest`.
- **API:** adds `GET /v3/api-docs`, `GET /swagger-ui.html` and Swagger UI static assets under `/swagger-ui/`. No change to `/api/v1`.
- **Dependencies:** springdoc 3.2.0, which brings in swagger-core 2.2.55 and the swagger-ui webjar.
- **Docs:**
  - `docs/STANDARDS.md` §1: pin the springdoc version. §4: generated docs must stay complete, and the test enforces it. Add a change log row.
  - `docs/PRD.md` §3.3: note the OpenAPI 3.1 version, and add a change log row.
  - `docs/BACKLOG.md`: set item 5 to Done.
  - `docs/DOMAIN-MODEL.md`: none.
