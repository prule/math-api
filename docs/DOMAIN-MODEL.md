# Math API — Domain Model

> Living document. Update it in the same change as any new or changed term, operation or business rule (see `CLAUDE.md`).
> Related: [PRD](PRD.md) · [Standards](STANDARDS.md)

The domain is framework-free Java in `com.paulrule.mathapi.domain`. Specs, code and tests cite business rules by ID (`BR-n`). Never renumber or reuse an ID; mark retired rules as *Retired*.

## 1. Glossary

| Term | Definition | Code |
|------|------------|------|
| **Operand** | A decimal number within the limits of BR-1. | `BigDecimal` (validated) |
| **Operation** | A named arithmetic function of two operands. Closed set. | `enum Operation` |
| **Calculation** | One Operation applied to two Operands `(operation, a, b)`. Immutable. | `record Calculation` |
| **Result** | The decimal value produced by performing a Calculation. | `BigDecimal` |

Use these words exactly in code, specs, API docs and conversation. Avoid synonyms ("operator", "argument", "answer").

## 2. Model

```
Calculation
  ├── operation : Operation   { ADD | SUBTRACT | MULTIPLY | DIVIDE }
  ├── a         : Operand
  └── b         : Operand

Operation.apply(a, b) → Result
```

- Each `Operation` constant implements `apply(BigDecimal a, BigDecimal b)`.
- Adding an operation = one new constant + its business rules here + tests. No other layer changes.
- `Operation.named(name)` is an exact, case-sensitive lookup; any other name raises `UnknownOperationException`.
- `Calculation.result()` applies the Operation and normalises the Result (BR-4 trailing-zero stripping). Plain-notation rendering happens at the web adapter.
- Domain failures are domain exceptions (`UnknownOperationException`; later `DivisionByZeroException`, `OperandOutOfRangeException`); the web adapter maps them to Problem Details.

## 3. Operations

| Operation | Rule | Edge cases that must be tested |
|-----------|------|--------------------------------|
| `ADD` | `a + b`, exact | `0.1 + 0.2 = 0.3`; negatives; large precision |
| `SUBTRACT` | `a − b`, exact | result zero; negative result |
| `MULTIPLY` | `a × b`, exact | × 0; sign rules; scale growth |
| `DIVIDE` | `a ÷ b` under BR-3 | BR-2; `1 ÷ 3` rounding; exact `1 ÷ 4 = 0.25`; negative divisor |

## 4. Business rules

| ID | Rule |
|----|------|
| **BR-1** | An Operand has at most **100 significant digits** (`precision() ≤ 100`) and a magnitude exponent within **±1000** (`-1000 ≤ scale() ≤ 1000`, so `1e1000` is the largest power of ten accepted). Otherwise the Calculation is rejected (operand out of range). |
| **BR-2** | Division by zero is rejected. Applies to any zero representation (`0`, `0.00`, `-0`). |
| **BR-3** | `DIVIDE` uses `MathContext.DECIMAL128`: 34 significant digits, `RoundingMode.HALF_EVEN`. `ADD`, `SUBTRACT`, `MULTIPLY` are exact (no rounding). |
| **BR-4** | Every Result is normalised with trailing zeros stripped and returned in plain (non-scientific) notation; zero is `0`. |

## 5. Change log

| Date | Change |
|------|--------|
| 2026-11-07 | Initial model: four operations, BR-1 – BR-4 |
| 2026-11-07 | `add-exact-arithmetic`: ADD, SUBTRACT, MULTIPLY implemented; `Operation.named`, `UnknownOperationException`, where BR-4 is applied |
