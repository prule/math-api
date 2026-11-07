# api-documentation Specification

## Purpose

Gives clients a machine-readable OpenAPI description and an interactive UI for the Math API's HTTP contract, generated from the running service so that it stays in step with the implementation.

## Requirements

### Requirement: OpenAPI document served
While API documentation is enabled (the default), the service SHALL answer `GET /v3/api-docs` with HTTP 200 and a JSON OpenAPI 3.1 document describing its public API, without requiring credentials.

#### Scenario: Document available
- **WHEN** API documentation is enabled and a client sends `GET /v3/api-docs`
- **THEN** the response status is 200
- **AND** the body is JSON whose `openapi` field starts with `3.1`

### Requirement: Interactive documentation served
While API documentation is enabled (the default), the service SHALL serve Swagger UI at `/swagger-ui.html`, rendering the OpenAPI document.

#### Scenario: UI reachable
- **WHEN** API documentation is enabled and a client sends `GET /swagger-ui.html` and follows redirects
- **THEN** the final response status is 200 with an HTML page

### Requirement: Calculation endpoint documented
The OpenAPI document SHALL describe `POST /api/v1/calculations` with an `application/json` request body and a 200 response of content type `application/json`. Each response field (`operation`, `a`, `b`, `result`) SHALL be typed as a string. The operation SHALL document exactly the responses `200`, `400` and `422`.

#### Scenario: Path and response shape
- **WHEN** a client reads the OpenAPI document
- **THEN** it contains the path `/api/v1/calculations` with a `post` operation
- **AND** the 200 response is listed under `application/json` (not `*/*`), with string properties `operation`, `a`, `b` and `result`

#### Scenario: Only the real responses are documented
- **WHEN** a client reads the calculation operation's responses
- **THEN** the response codes are exactly `200`, `400` and `422`

### Requirement: Operands documented as number or string
The request schema SHALL type `a` and `b` as either a number or a string, and mark `operation`, `a` and `b` as required.

#### Scenario: Operand types
- **WHEN** a client reads the request schema
- **THEN** `a` and `b` each allow both `number` and `string`
- **AND** `operation`, `a` and `b` are listed as required

### Requirement: Allowed operations documented from the supported set
The request schema SHALL list `operation`'s allowed values as exactly the names of the supported Operations, case-sensitive, so the documentation changes whenever an Operation is added or removed.

#### Scenario: Allowed values match supported Operations
- **WHEN** a client reads the request schema
- **THEN** `operation`'s allowed values are exactly `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `POWER`, `MODULO`

### Requirement: Error responses documented
The calculation operation SHALL document a 400 response and a 422 response, each with content type `application/problem+json` and a Problem Details schema. The 400 response SHALL include one example per `type` (`/problems/invalid-request`, `/problems/unknown-operation`, `/problems/operand-out-of-range`), and the 422 response one example per `type` (`/problems/division-by-zero`, `/problems/invalid-exponent`, `/problems/result-out-of-range`).

#### Scenario: Each error type has an example
- **WHEN** a client reads the calculation operation's responses
- **THEN** response `400` has `application/problem+json` examples whose `type` values are `/problems/invalid-request`, `/problems/unknown-operation` and `/problems/operand-out-of-range`
- **AND** response `422` has `application/problem+json` examples whose `type` values are `/problems/division-by-zero`, `/problems/invalid-exponent` and `/problems/result-out-of-range`

### Requirement: API documentation can be disabled per environment
API documentation SHALL be enabled by default and disabled when the service runs with the `prod` profile. While disabled, `GET /v3/api-docs`, `GET /swagger-ui.html`, any path under `/swagger-ui/` and the Swagger UI webjar under `/webjars/swagger-ui/` SHALL return HTTP 404, and all other endpoints SHALL behave exactly as when documentation is enabled.

#### Scenario: Enabled by default
- **WHEN** the service starts with no active profile
- **THEN** `GET /v3/api-docs` returns 200 and `GET /swagger-ui.html` redirects to a 200 HTML page

#### Scenario: Disabled under the prod profile
- **WHEN** the service starts with the `prod` profile active
- **THEN** `GET /v3/api-docs`, `GET /swagger-ui.html`, `GET /swagger-ui/index.html` and `GET /webjars/swagger-ui/index.html` each return 404

#### Scenario: Calculation API unaffected when docs are disabled
- **WHEN** the service starts with the `prod` profile active and a client requests `MULTIPLY` of `"1.5"` and `"1.5"`
- **THEN** the response status is 200 and the `result` is `"2.25"`
- **AND** `GET /actuator/health` returns 200 with `status` `UP`

### Requirement: Unknown-operation example lists the supported operations
The documented `/problems/unknown-operation` example SHALL list exactly the names of the supported Operations, in the same order the service reports them, so the example cannot drift from the real response when an Operation is added.

#### Scenario: Example matches the supported set
- **WHEN** a client reads the 400 response's `unknown-operation` example
- **THEN** its `detail` lists exactly `ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, MODULO`
