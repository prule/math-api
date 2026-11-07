# Spec Delta

## ADDED Requirements

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
3. operand limits BR-1 (`operand-out-of-range`)
4. division by zero BR-2 (`division-by-zero`)

#### Scenario: Unknown operation wins over operand limits
- **WHEN** a client posts `{ "operation": "NOT_AN_OPERATION", "a": "1E+1001", "b": "1" }`
- **THEN** the `type` is `/problems/unknown-operation`

#### Scenario: Operand limits win over division by zero
- **WHEN** a client requests `DIVIDE` of `"1E+1001"` and `"0"`
- **THEN** the response status is 400 and the `type` is `/problems/operand-out-of-range`
