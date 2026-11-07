# Spec Delta

## ADDED Requirements

### Requirement: DIVIDE rounds to 34 significant digits
`DIVIDE` SHALL return `a ÷ b` computed to 34 significant digits with HALF_EVEN rounding (BR-3). Quotients that fit within 34 significant digits SHALL be exact. The Result SHALL then be normalised (BR-4).

#### Scenario: Exact quotient
- **WHEN** a client requests `DIVIDE` of `"1"` and `"4"`
- **THEN** the `result` is `"0.25"`

#### Scenario: Non-terminating quotient rounded
- **WHEN** a client requests `DIVIDE` of `"1"` and `"3"`
- **THEN** the `result` is `"0.3333333333333333333333333333333333"`

#### Scenario: Last digit rounds up
- **WHEN** a client requests `DIVIDE` of `"2"` and `"3"`
- **THEN** the `result` is `"0.6666666666666666666666666666666667"`

#### Scenario: Ties round to even
- **WHEN** a client requests `DIVIDE` of `"1.0000000000000000000000000000000005"` and `"1"`
- **THEN** the `result` is `"1"`
- **AND WHEN** a client requests `DIVIDE` of `"1.0000000000000000000000000000000015"` and `"1"`
- **THEN** the `result` is `"1.000000000000000000000000000000002"`

#### Scenario: Negative divisor
- **WHEN** a client requests `DIVIDE` of `"1"` and `"-4"`
- **THEN** the `result` is `"-0.25"`

#### Scenario: Zero dividend
- **WHEN** a client requests `DIVIDE` of `"0"` and `"5"`
- **THEN** the `result` is `"0"`

### Requirement: Division by zero rejected
A `DIVIDE` whose divisor `b` is zero in any representation SHALL be rejected with HTTP 422 and a Problem Details body whose `type` is `/problems/division-by-zero` (BR-2).

#### Scenario: Zero divisor
- **WHEN** a client posts `{ "operation": "DIVIDE", "a": "1", "b": "0" }`
- **THEN** the response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: Other zero representations
- **WHEN** a client requests `DIVIDE` with `b` given as `"0.00"`, `"-0"`, `"0E+5"` or the JSON number `0.0`
- **THEN** each response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: Zero divided by zero
- **WHEN** a client requests `DIVIDE` of `"0"` and `"0"`
- **THEN** the response status is 422 and the `type` is `/problems/division-by-zero`
