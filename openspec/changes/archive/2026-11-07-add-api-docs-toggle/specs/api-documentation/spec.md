# Spec Delta

## MODIFIED Requirements

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

## ADDED Requirements

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
