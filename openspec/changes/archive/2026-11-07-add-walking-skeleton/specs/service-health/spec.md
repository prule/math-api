# Spec Delta

## Purpose

Lets callers and orchestrators find out over HTTP whether the Math API service is running and able to serve requests, without exposing any other operational detail.

## ADDED Requirements

### Requirement: Health check reports service status
The service SHALL answer `GET /actuator/health` with HTTP 200 and a JSON body whose `status` is `UP` when the service has started and can serve requests.

#### Scenario: Running service reports UP
- **WHEN** a client sends `GET /actuator/health` to a started service
- **THEN** the response status is 200
- **AND** the response body is JSON containing `"status": "UP"`

### Requirement: Health check needs no authentication
The health check SHALL be reachable without credentials, consistent with the PRD's v1 non-goal of no authentication.

#### Scenario: Anonymous health request
- **WHEN** a client sends `GET /actuator/health` with no credentials or authorization header
- **THEN** the response status is 200

### Requirement: Health check exposes no internal detail
The health response SHALL report the overall status without per-component health. Component details (for example disk space or JVM information) SHALL NOT appear in the response.

#### Scenario: No component details in response
- **WHEN** a client sends `GET /actuator/health`
- **THEN** the response body has no `components` or `details` field

### Requirement: No other operational endpoints over HTTP
Apart from the health check, the service SHALL NOT expose operational endpoints (environment, configuration, beans, metrics, heap dumps, etc.) over HTTP.

#### Scenario: Environment endpoint is not exposed
- **WHEN** a client sends `GET /actuator/env`
- **THEN** the response status is 404

#### Scenario: Actuator index does not list hidden endpoints
- **WHEN** a client sends `GET /actuator`
- **THEN** the only links in the response are `self` and the health links (`health`, `health-path`)
