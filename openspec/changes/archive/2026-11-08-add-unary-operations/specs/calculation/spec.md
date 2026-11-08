# Spec Delta

## MODIFIED Requirements

### Requirement: Perform a Calculation
The service SHALL accept `POST /api/v1/calculations` with a JSON body holding `operation`, `a` and, for a binary Operation, `b`. It SHALL answer HTTP 200 with a JSON body holding `operation`, `a`, `result` and, for a binary Operation, `b`, where `a`, `b` and `result` are JSON strings. For a unary Operation the response body SHALL NOT contain a `b` key.

#### Scenario: Successful Calculation
- **WHEN** a client posts `{ "operation": "ADD", "a": "2", "b": "3" }`
- **THEN** the response status is 200
- **AND** the body is `{ "operation": "ADD", "a": "2", "b": "3", "result": "5" }`

#### Scenario: Successful unary Calculation
- **WHEN** a client posts `{ "operation": "NEGATE", "a": "2" }`
- **THEN** the response status is 200
- **AND** the body is exactly `{ "operation": "NEGATE", "a": "2", "result": "-2" }`

### Requirement: Invalid request rejected
A request with malformed JSON, a missing or null `operation` or `a`, a missing or null `b` for a binary Operation, or an Operand that isn't a decimal number SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/invalid-request`.

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

#### Scenario: Missing operand for a unary Operation
- **WHEN** a client posts `{ "operation": "SQRT" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

### Requirement: Validation order
A request SHALL be checked in this order, and only the first failure SHALL be reported:
1. well-formed request (`invalid-request`)
2. known Operation (`unknown-operation`)
3. Operand count matches the Operation, BR-9 (`invalid-request`)
4. operand limits BR-1 (`operand-out-of-range`)
5. exponent rule BR-5 (`invalid-exponent`)
6. division by zero BR-2 (`division-by-zero`)
7. square root of a negative BR-8 (`negative-square-root`)
8. result bound BR-6 (`result-out-of-range`)

#### Scenario: Unknown operation wins over operand limits
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "1E+1001", "b": "1" }`
- **THEN** the `type` is `/problems/unknown-operation`

#### Scenario: Unknown operation wins over operand count
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "1" }`
- **THEN** the response status is 400 and the `type` is `/problems/unknown-operation`

#### Scenario: Operand count wins over operand limits
- **WHEN** a client posts `{ "operation": "SQRT", "a": "4", "b": "1E+1001" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Operand limits win over division by zero
- **WHEN** a client requests `DIVIDE` of `"1E+1001"` and `"0"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`

#### Scenario: Operand limits win over square root of a negative
- **WHEN** a client requests `SQRT` of `"-1E+1001"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`

#### Scenario: Exponent rule wins over zero to a negative power
- **WHEN** a client requests `POWER` of `"0"` and `"-0.5"`
- **THEN** the response status is 422 and the `type` is `/problems/invalid-exponent`

## ADDED Requirements

### Requirement: Operand count matches the Operation
Every Operation SHALL be unary (one Operand, `a`) or binary (Operands `a` and `b`) (BR-9). `SQRT` and `NEGATE` SHALL be unary; `ADD`, `SUBTRACT`, `MULTIPLY`, `DIVIDE`, `POWER` and `MODULO` SHALL be binary. A request that gives `b` to a unary Operation SHALL be rejected with HTTP 400, `type` `/problems/invalid-request`, and a `detail` naming `b`. A `b` of JSON `null` SHALL count as omitted.

#### Scenario: b given to a unary Operation
- **WHEN** a client posts `{ "operation": "SQRT", "a": "4", "b": "1" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`
- **AND** the `detail` contains `'b'`

#### Scenario: Null b counts as omitted
- **WHEN** a client posts `{ "operation": "SQRT", "a": "4", "b": null }`
- **THEN** the response status is 200 and the body is exactly `{ "operation": "SQRT", "a": "4", "result": "2" }`

#### Scenario: b missing for a binary Operation names b
- **WHEN** a client posts `{ "operation": "ADD", "a": "1" }`
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`
- **AND** the `detail` contains `'b'`

### Requirement: NEGATE is exact
`NEGATE` SHALL return `−a` with no rounding (BR-3). The Result SHALL be normalised (BR-4), so every zero representation negates to `"0"`.

#### Scenario: Positive and negative Operands
- **WHEN** a client requests `NEGATE` of `"5"`, and then of `"-2.50"`
- **THEN** the `result` is `"-5"`, and then `"2.5"`
- **AND** the second response echoes `a` as `"-2.50"`

#### Scenario: Zero
- **WHEN** a client requests `NEGATE` of `"0"`, `"-0"` and `"0.00"`
- **THEN** each `result` is `"0"`

### Requirement: SQRT rounds to 34 significant digits
`SQRT` SHALL return the square root of `a`, correctly rounded to 34 significant digits with HALF_EVEN rounding (BR-3), then normalised (BR-4). A square root that fits within 34 significant digits SHALL be exact.

#### Scenario: Exact square roots
- **WHEN** a client requests `SQRT` of `"4"`, of `"0.25"` and of `"1E+1000"`
- **THEN** the `result` is `"2"`, `"0.5"`, and `"1"` followed by 500 zeros

#### Scenario: Irrational root rounded down
- **WHEN** a client requests `SQRT` of `"2"`
- **THEN** the `result` is `"1.414213562373095048801688724209698"`

#### Scenario: Last digit rounds up
- **WHEN** a client requests `SQRT` of `"10"`
- **THEN** the `result` is `"3.162277660168379331998893544432719"`

#### Scenario: Ties round to even
- **WHEN** a client requests `SQRT` of `"1.00000000000000000000000000000000100000000000000000000000000000000025"` (exact root `1.0000000000000000000000000000000005`), and then of `"1.00000000000000000000000000000000300000000000000000000000000000000225"` (exact root `1.0000000000000000000000000000000015`)
- **THEN** the `result` is `"1"`, and then `"1.000000000000000000000000000000002"`

#### Scenario: Square root of zero
- **WHEN** a client requests `SQRT` of `"0"`, `"-0"` and `"0.00"`
- **THEN** each response status is 200 and each `result` is `"0"`

### Requirement: Square root of a negative rejected
A `SQRT` whose `a` is less than zero SHALL be rejected with HTTP 422 and a Problem Details body whose `type` is `/problems/negative-square-root` (BR-8).

#### Scenario: Negative Operand
- **WHEN** a client requests `SQRT` of `"-1"`, and then of `"-0.0001"`
- **THEN** each response status is 422 and each `type` is `/problems/negative-square-root`
