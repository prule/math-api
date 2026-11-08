# Spec Delta

## MODIFIED Requirements

### Requirement: Operands documented as number or string
The request schema SHALL type `a` and `b` as either a number or a string, and mark only `operation` and `a` as required. The description of `b` SHALL say it is required for binary Operations and must be omitted for unary ones.

#### Scenario: Operand types
- **WHEN** a client reads the request schema
- **THEN** `a` and `b` each allow both `number` and `string`
- **AND** exactly `operation` and `a` are listed as required
- **AND** `b`'s description mentions both binary and unary Operations

### Requirement: Allowed operations documented from the supported set
The request schema SHALL list `operation`'s allowed values as exactly the names of the supported Operations, case-sensitive, so the documentation changes whenever an Operation is added or removed.

#### Scenario: Allowed values match supported Operations
- **WHEN** a client reads the request schema
- **THEN** `operation`'s allowed values are exactly `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `POWER`, `MODULO`, `SQRT`, `NEGATE`

### Requirement: Error responses documented
The calculation operation SHALL document a 400 response and a 422 response, each with content type `application/problem+json` and a Problem Details schema. The 400 response SHALL include one example per `type` (`/problems/invalid-request`, `/problems/unknown-operation`, `/problems/operand-out-of-range`), and the 422 response one example per `type` (`/problems/division-by-zero`, `/problems/invalid-exponent`, `/problems/negative-square-root`, `/problems/result-out-of-range`).

#### Scenario: Each error type has an example
- **WHEN** a client reads the calculation operation's responses
- **THEN** response `400` has `application/problem+json` examples whose `type` values are `/problems/invalid-request`, `/problems/unknown-operation` and `/problems/operand-out-of-range`
- **AND** response `422` has `application/problem+json` examples whose `type` values are `/problems/division-by-zero`, `/problems/invalid-exponent`, `/problems/negative-square-root` and `/problems/result-out-of-range`

### Requirement: Unknown-operation example lists the supported operations
The documented `/problems/unknown-operation` example SHALL list exactly the names of the supported Operations, in the same order the service reports them, so the example cannot drift from the real response when an Operation is added.

#### Scenario: Example matches the supported set
- **WHEN** a client reads the 400 response's `unknown-operation` example
- **THEN** its `detail` lists exactly `ADD, SUBTRACT, MULTIPLY, DIVIDE, POWER, MODULO, SQRT, NEGATE`
