# Spec Delta

## ADDED Requirements

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

## MODIFIED Requirements

### Requirement: Unknown Operation rejected
A request whose `operation` is not exactly the name of a supported Operation (names are case-sensitive) SHALL be rejected with HTTP 400 and a Problem Details body whose `type` is `/problems/unknown-operation`.

#### Scenario: Unsupported name
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "2", "b": "3" }`
- **THEN** the response status is 400 and the `type` is `/problems/unknown-operation`

#### Scenario: Wrong case
- **WHEN** a client posts `{ "operation": "add", "a": "2", "b": "3" }`
- **THEN** the response status is 400 and the `type` is `/problems/unknown-operation`

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

### Requirement: Validation order
A request SHALL be checked in this order, and only the first failure SHALL be reported:
1. well-formed request (`invalid-request`)
2. known Operation (`unknown-operation`)
3. operand limits BR-1 (`operand-out-of-range`)
4. exponent rule BR-5 (`invalid-exponent`)
5. division by zero BR-2 (`division-by-zero`)
6. result bound BR-6 (`result-out-of-range`)

#### Scenario: Unknown operation wins over operand limits
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "1E+1001", "b": "1" }`
- **THEN** the `type` is `/problems/unknown-operation`

#### Scenario: Operand limits win over division by zero
- **WHEN** a client requests `DIVIDE` of `"1E+1001"` and `"0"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`

#### Scenario: Exponent rule wins over zero to a negative power
- **WHEN** a client requests `POWER` of `"0"` and `"-0.5"`
- **THEN** the response status is 422 and the `type` is `/problems/invalid-exponent`
