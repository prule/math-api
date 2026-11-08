# Spec Delta

## ADDED Requirements

### Requirement: Operations endpoint documented
The OpenAPI document SHALL describe `GET /api/v1/operations` with exactly one response, `200`, of content type `application/json`, whose schema is an array of objects with a string property `name` and an integer property `arity`.

#### Scenario: Path and response shape
- **WHEN** a client reads the OpenAPI document
- **THEN** it contains the path `/api/v1/operations` with a `get` operation
- **AND** its response codes are exactly `200`
- **AND** the 200 response is listed under `application/json` (not `*/*`) as an array whose items have a string `name` and an integer `arity`
