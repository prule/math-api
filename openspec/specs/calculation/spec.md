# calculation Specification

## Purpose

Lets a client perform one Operation on its Operands (one or two, per its arity) over HTTP and receive an exact decimal Result. It also defines how invalid Calculations are rejected with RFC 9457 Problem Details.

## Requirements

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

### Requirement: Unknown Operation rejected
A request whose `operation` is not exactly the name of a supported Operation (names are case-sensitive) SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/unknown-operation`.

#### Scenario: Unsupported name
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "2", "b": "3" }`
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
A `DIVIDE` or `MODULO` whose divisor `b` is zero in any representation, and a `POWER` of zero to a negative exponent, SHALL be rejected with HTTP 422 and a Problem Details body whose `type` is `/problems/division-by-zero` (BR-2).

#### Scenario: Zero divisor
- **WHEN** a client posts `{ "operation": "DIVIDE", "a": "1", "b": "0" }`
- **THEN** the response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: Other zero representations
- **WHEN** a client requests `DIVIDE` with `b` given as `"0.00"`, `"-0"`, `"0E+5"` or the JSON number `0.0`
- **THEN** each response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: Zero divided by zero
- **WHEN** a client requests `DIVIDE` of `"0"` and `"0"`
- **THEN** the response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: MODULO by zero
- **WHEN** a client requests `MODULO` of `"7"` and `"0"`, or of `"7"` and `"0.00"`
- **THEN** each response status is 422 and the `type` is `/problems/division-by-zero`

#### Scenario: Zero to a negative power
- **WHEN** a client requests `POWER` of `"0"` and `"-1"`
- **THEN** the response status is 422 and the `type` is `/problems/division-by-zero`

### Requirement: Operand digits limited
Each Operand, as written, SHALL have at most 100 significant digits (BR-1). An Operand with more SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/operand-out-of-range`.

#### Scenario: Exactly 100 digits accepted
- **WHEN** a client requests `ADD` with `a` a 100-digit integer and `b` `"0"`
- **THEN** the response status is 200 and the `result` equals `a`

#### Scenario: 101 digits rejected
- **WHEN** a client requests `ADD` with `a` a 101-digit integer and `b` `"0"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`

#### Scenario: Trailing zeros written out count as digits
- **WHEN** a client requests `ADD` with `a` written as `1` followed by 100 zeros
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`
- **AND WHEN** the same value is written as `"1E+100"`
- **THEN** the response status is 200

### Requirement: Operand scale limited
Each Operand, as written, SHALL have a scale between -1000 and 1000 inclusive (BR-1), i.e. at most 1000 decimal places, and its last written digit no higher than the 10^1000 place. An Operand outside this range SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/operand-out-of-range`.

#### Scenario: Scale limits accepted
- **WHEN** a client requests `ADD` of `"1E+1000"` and `"1E-1000"`
- **THEN** the response status is 200

#### Scenario: One past each scale limit rejected
- **WHEN** a client requests `ADD` with `a` `"1E+1001"`, or separately with `b` `"1E-1001"`
- **THEN** each response status is 400 and the `type` is `/problems/operand-out-of-range`

#### Scenario: Extreme exponents rejected before any arithmetic
- **WHEN** a client requests `MULTIPLY` of `1e999999999` and `1e999999999`, or `DIVIDE` of `"1e-999999999"` and `"1e999999999"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`
- **AND** the response arrives within 1 second

### Requirement: Out-of-range detail names the operand
The Problem Details `detail` for an out-of-range Operand SHALL name the offending operand (`a` or `b`) and the limit it broke, without echoing the operand's value.

#### Scenario: Detail identifies operand and limit
- **WHEN** a client requests `ADD` with `a` `"1"` and `b` `"1E+1001"`
- **THEN** the `detail` names `b` and the scale limit
- **AND** the `detail` does not contain `1E+1001`

### Requirement: Request parsing is bounded
The service SHALL reject, with HTTP 400 and `type` `/problems/invalid-request`, any request body longer than 16,384 characters, any JSON string longer than 1,000 characters, and any JSON number longer than 1,000 characters, without parsing such tokens into numbers.

#### Scenario: Oversized body
- **WHEN** a client posts a valid calculation padded with an extra field to more than 16,384 characters
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Oversized string operand
- **WHEN** a client posts `a` as a JSON string of 1,001 digits
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Oversized number operand
- **WHEN** a client posts `a` as a JSON number of 1,001 digits
- **THEN** the response status is 400 and the `type` is `/problems/invalid-request`

#### Scenario: Large but parseable operand reaches the domain rule
- **WHEN** a client posts `a` as a JSON string of 150 digits
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`

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

### Requirement: POWER raises to an integer exponent
`POWER` SHALL return `a` raised to the integer power `b`: the exact value rounded once to 34 significant digits with HALF_EVEN rounding (BR-3), then normalised (BR-4). A negative `b` SHALL give `1 / a^|b|` rounded the same way, and `a^0` SHALL be `1` for every `a`, including zero.

#### Scenario: Exact small power
- **WHEN** a client requests `POWER` of `"2"` and `"10"`
- **THEN** the `result` is `"1024"`

#### Scenario: Negative exponent
- **WHEN** a client requests `POWER` of `"2"` and `"-2"`
- **THEN** the `result` is `"0.25"`
- **AND WHEN** a client requests `POWER` of `"3"` and `"-1"`
- **THEN** the `result` is `"0.3333333333333333333333333333333333"`

#### Scenario: Negative base and decimal base
- **WHEN** a client requests `POWER` of `"-2"` and `"3"`
- **THEN** the `result` is `"-8"`
- **AND WHEN** a client requests `POWER` of `"1.1"` and `"2"`
- **THEN** the `result` is `"1.21"`

#### Scenario: Large result rounded to 34 digits
- **WHEN** a client requests `POWER` of `"2"` and `"200"`
- **THEN** the `result` is `"1606938044258990275541962092341163000000000000000000000000000"`

#### Scenario: Zero exponent
- **WHEN** a client requests `POWER` of `"0"` and `"0"`, or of `"5"` and `"0"`
- **THEN** each `result` is `"1"`

#### Scenario: Integer-valued decimal exponent accepted
- **WHEN** a client requests `POWER` of `"2"` and `"3.0"`
- **THEN** the `result` is `"8"`

### Requirement: POWER exponent must be a bounded integer
`POWER` SHALL reject an exponent `b` that is not an integer or whose absolute value exceeds 999 (BR-5) with HTTP 422 and a Problem Details body whose `type` is `/problems/invalid-exponent`.

#### Scenario: Fractional exponent
- **WHEN** a client requests `POWER` of `"2"` and `"0.5"`
- **THEN** the response status is 422 and the `type` is `/problems/invalid-exponent`

#### Scenario: Exponent past the bound
- **WHEN** a client requests `POWER` of `"2"` and `"1000"`, or of `"2"` and `"-1000"`
- **THEN** each response status is 422 and the `type` is `/problems/invalid-exponent`

#### Scenario: Exponent at the bound
- **WHEN** a client requests `POWER` of `"10"` and `"999"`, or of `"0.1"` and `"999"`
- **THEN** each response status is 200

### Requirement: POWER result is bounded
A `POWER` Result whose scale, after rounding and normalisation (BR-4), is outside -1000 to 1000 (BR-6) SHALL be rejected with HTTP 422 and a Problem Details body whose `type` is `/problems/result-out-of-range`.

#### Scenario: Result too large
- **WHEN** a client requests `POWER` of `"100"` and `"999"`
- **THEN** the response status is 422 and the `type` is `/problems/result-out-of-range`

#### Scenario: Result too small
- **WHEN** a client requests `POWER` of `"0.01"` and `"999"`
- **THEN** the response status is 422 and the `type` is `/problems/result-out-of-range`

#### Scenario: Worst-case operands stay fast
- **WHEN** a client requests `POWER` with `a` the 100-digit operand `"9.99…9E-901"` (one hundred 9s, scale 1000, written in E notation) and `b` `"999"`, or the same `a` with `b` `"-999"`
- **THEN** each response status is 422, the `type` is `/problems/result-out-of-range`, and each response arrives within 1 second
- **AND WHEN** a client requests `POWER` of `"1.00…01"` (100 significant digits: a 1, ninety-eight 0s, a 1) and `"999"`
- **THEN** the response status is 200 with `result` `"1"`, within 1 second

### Requirement: MODULO is a truncated remainder
`MODULO` SHALL return the exact remainder of `a ÷ b` truncated toward zero, so the Result takes the sign of `a` (BR-7), normalised (BR-4).

#### Scenario: Sign follows the dividend
- **WHEN** a client requests `MODULO` of `"-7"` and `"3"`
- **THEN** the `result` is `"-1"`
- **AND WHEN** a client requests `MODULO` of `"7"` and `"-3"`
- **THEN** the `result` is `"1"`

#### Scenario: Decimal operands
- **WHEN** a client requests `MODULO` of `"5.5"` and `"2"`
- **THEN** the `result` is `"1.5"`
- **AND WHEN** a client requests `MODULO` of `"-7.5"` and `"2"`
- **THEN** the `result` is `"-1.5"`

#### Scenario: Exact multiple and large dividend
- **WHEN** a client requests `MODULO` of `"6"` and `"3"`
- **THEN** the `result` is `"0"`
- **AND WHEN** a client requests `MODULO` of `"1E+1000"` and `"7"`
- **THEN** the `result` is `"4"`

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
