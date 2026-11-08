# operation-discovery Specification

## Purpose
Lets a client discover over HTTP which Operations the service supports and the Arity of each, so it can build valid Calculation requests without hard-coding the list.

## Requirements

### Requirement: List the supported Operations
The service SHALL answer `GET /api/v1/operations` with HTTP 200 and an `application/json` array holding exactly one entry per supported Operation, in the same order the service lists them in an `unknown-operation` detail. It SHALL require no credentials and take no request body or parameters.

#### Scenario: Every supported Operation listed in order
- **WHEN** a client sends `GET /api/v1/operations`
- **THEN** the response status is 200 with content type `application/json`
- **AND** the body is an array whose entries' `name` values are exactly `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `POWER`, `MODULO`, `SQRT`, `NEGATE`, in that order

### Requirement: Each entry gives the name and Arity
Each entry SHALL be an object with exactly two keys: `name`, a string that is the case-sensitive value a client sends as `operation`, and `arity`, a JSON integer that is the Operation's number of Operands, 1 or 2 (BR-9).

#### Scenario: Binary and unary entries
- **WHEN** a client sends `GET /api/v1/operations`
- **THEN** the entry for `ADD` is `{ "name": "ADD", "arity": 2 }`
- **AND** the entry for `SQRT` is `{ "name": "SQRT", "arity": 1 }`
- **AND** the entry for `NEGATE` is `{ "name": "NEGATE", "arity": 1 }`

#### Scenario: Listed names are accepted by the calculation endpoint
- **WHEN** a client takes any listed entry and sends `POST /api/v1/calculations` with that `name` as `operation`, `a` of `"4"`, and `b` of `"2"` only when its `arity` is 2
- **THEN** the response status is 200

### Requirement: Other methods rejected
The service SHALL serve `GET` and `HEAD` on `/api/v1/operations`; any other method SHALL get HTTP 405 as Problem Details.

#### Scenario: POST not allowed
- **WHEN** a client sends `POST /api/v1/operations`
- **THEN** the response status is 405 with content type `application/problem+json`
