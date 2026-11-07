# Spec Delta

## Purpose

Lets a client perform one Operation on two Operands over HTTP and receive an exact decimal Result. It also defines how invalid Calculations are rejected with RFC 9457 Problem Details.

## ADDED Requirements

### Requirement: Perform a Calculation
The service SHALL accept `POST /api/v1/calculations` with a JSON body holding `operation`, `a` and `b`. It SHALL answer HTTP 200 with a JSON body holding `operation`, `a`, `b` and `result`, where `a`, `b` and `result` are JSON strings.

#### Scenario: Successful Calculation
- **WHEN** a client posts `{ "operation": "ADD", "a": "2", "b": "3" }`
- **THEN** the response status is 200
- **AND** the body is `{ "operation": "ADD", "a": "2", "b": "3", "result": "5" }`

### Requirement: Operands accepted as numbers or strings
Each Operand SHALL be accepted either as a JSON number or as a JSON string holding a decimal number. Both forms SHALL give the same Result. A JSON number SHALL be read exactly, with no binary floating-point rounding.

#### Scenario: Number and string operands
- **WHEN** a client posts `{ "operation": "ADD", "a": 1.5, "b": "2.5" }`
- **THEN** the `result` is `"4"`

#### Scenario: JSON number read exactly
- **WHEN** a client posts `{ "operation": "ADD", "a": 0.1, "b": 0.2 }`
- **THEN** the `result` is `"0.3"`

### Requirement: Operands echoed in plain notation
The response SHALL echo `a` and `b` as the decimal values received, written in plain (non-scientific) notation and keeping their given scale.

#### Scenario: Scientific input echoed plainly
- **WHEN** a client posts `{ "operation": "ADD", "a": 1e2, "b": "1.50" }`
- **THEN** the body's `a` is `"100"` and `b` is `"1.50"`

### Requirement: ADD is exact
`ADD` SHALL return `a + b` with no rounding (BR-3).

#### Scenario: Decimal fractions add exactly
- **WHEN** a client requests `ADD` of `"0.1"` and `"0.2"`
- **THEN** the `result` is `"0.3"`

#### Scenario: Negative operands
- **WHEN** a client requests `ADD` of `"-5"` and `"3"`
- **THEN** the `result` is `"-2"`

#### Scenario: Large precision kept
- **WHEN** a client requests `ADD` of `"12345678901234567890.123456789"` and `"0.000000001"`
- **THEN** the `result` is `"12345678901234567890.12345679"`

### Requirement: SUBTRACT is exact
`SUBTRACT` SHALL return `a − b` with no rounding (BR-3).

#### Scenario: Result is zero
- **WHEN** a client requests `SUBTRACT` of `"1.10"` and `"1.1"`
- **THEN** the `result` is `"0"`

#### Scenario: Negative result
- **WHEN** a client requests `SUBTRACT` of `"2"` and `"5.5"`
- **THEN** the `result` is `"-3.5"`

### Requirement: MULTIPLY is exact
`MULTIPLY` SHALL return `a × b` with no rounding (BR-3).

#### Scenario: Multiply by zero
- **WHEN** a client requests `MULTIPLY` of `"123.45"` and `"0"`
- **THEN** the `result` is `"0"`

#### Scenario: Sign rules
- **WHEN** a client requests `MULTIPLY` of `"-2"` and `"-3"`
- **THEN** the `result` is `"6"`
- **AND WHEN** a client requests `MULTIPLY` of `"-2"` and `"3"`
- **THEN** the `result` is `"-6"`

#### Scenario: Scale grows exactly
- **WHEN** a client requests `MULTIPLY` of `"1.5"` and `"1.5"`
- **THEN** the `result` is `"2.25"`

### Requirement: Results are normalised
Every Result SHALL have trailing zeros stripped and be written in plain (non-scientific) notation, and a zero Result SHALL be `"0"` (BR-4).

#### Scenario: Trailing zeros stripped
- **WHEN** a client requests `MULTIPLY` of `"0.10"` and `"10"`
- **THEN** the `result` is `"1"`

#### Scenario: No scientific notation
- **WHEN** a client requests `MULTIPLY` of `"1000"` and `"1000"`
- **THEN** the `result` is `"1000000"`

### Requirement: Invalid request rejected
A request with malformed JSON, a missing or null `operation`, `a` or `b`, or an Operand that isn't a decimal number SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/invalid-request`.

#### Scenario: Malformed JSON
- **WHEN** a client posts `{ "operation": "ADD", "a": 1, `
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Missing operand
- **WHEN** a client posts `{ "operation": "ADD", "a": "1" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Missing operation
- **WHEN** a client posts `{ "a": "1", "b": "2" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Non-numeric operand
- **WHEN** a client posts `{ "operation": "ADD", "a": "abc", "b": "2" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

### Requirement: Unknown Operation rejected
A request whose `operation` is not exactly the name of a supported Operation (names are case-sensitive) SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/unknown-operation`.

#### Scenario: Unsupported name
- **WHEN** a client posts `{ "operation": "POWER", "a": "2", "b": "3" }`
- **THEN** the response status is 400 and the `type` is `/problems/unknown-operation`

#### Scenario: Wrong case
- **WHEN** a client posts `{ "operation": "add", "a": "2", "b": "3" }`
- **THEN** the response status is 400 and the `type` is `/problems/unknown-operation`

### Requirement: Error responses use Problem Details
Every error response from the calculation endpoint SHALL have content type `application/problem+json` and hold `type`, `title`, `status` and a human-readable `detail`. The `detail` SHALL NOT expose stack traces, exception class names or parser internals.

#### Scenario: Problem Details shape
- **WHEN** any request to `POST /api/v1/calculations` is rejected
- **THEN** the content type is `application/problem+json`
- **AND** the body has `type`, `title`, `status` matching the HTTP status, and `detail`
- **AND** `detail` contains no Java class name (e.g. no `tools.jackson` or `Exception`)
