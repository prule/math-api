# Spec Delta

## Purpose

Gives clients a machine-readable OpenAPI description and an interactive UI for the Math API's HTTP contract, generated from the running service so that it stays in step with the implementation.

## ADDED Requirements

### Requirement: OpenAPI document served
The service SHALL answer `GET /v3/api-docs` with HTTP 200 and a JSON OpenAPI 3.1 document describing its public API, without requiring credentials.

#### Scenario: Document available
- **WHEN** a client sends `GET /v3/api-docs`
- **THEN** the response status is 200
- **AND** the body is JSON whose `openapi` field starts with `3.1`

### Requirement: Interactive documentation served
The service SHALL serve Swagger UI at `/swagger-ui.html`, rendering the OpenAPI document.

#### Scenario: UI reachable
- **WHEN** a client sends `GET /swagger-ui.html` and follows redirects
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
- **THEN** `operation`'s allowed values are exactly `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`

### Requirement: Error responses documented
The calculation operation SHALL document a 400 response and a 422 response, each with content type `application/problem+json` and a Problem Details schema. The 400 response SHALL include one example per `type` (`/problems/invalid-request`, `/problems/unknown-operation`, `/problems/operand-out-of-range`), and the 422 response an example for `/problems/division-by-zero`.

#### Scenario: Each error type has an example
- **WHEN** a client reads the calculation operation's responses
- **THEN** response `400` has `application/problem+json` examples whose `type` values are `/problems/invalid-request`, `/problems/unknown-operation` and `/problems/operand-out-of-range`
- **AND** response `422` has an `application/problem+json` example whose `type` is `/problems/division-by-zero`
