# Design

## Context

- `Operation` is an enum whose constants implement `apply(a, b)`.
- `Calculation`'s constructor enforces BR-1 on both operands, and `result()` strips trailing zeros (BR-4).
- `ProblemDetailsAdvice` maps each domain exception to one Problem Details `type`.
- The OpenAPI 422 response currently documents only `division-by-zero`, and `ProblemExamples.UNKNOWN_OPERATION` lists the four operations by hand (v2 audit finding 2).
- Measured on this machine's JDK 25, warm:
  - exact `a.pow(999)` of the worst BR-1 operand (100 digits, scale 1000), then `round(DECIMAL128)`: **7 ms**
  - `a.pow(999, MathContext(44))` then round: 0.07 ms
  - worst `remainder` (a = 9…9E+1000, b = 1E-1000): 0.3 ms
  - negative exponent, `ONE.divide(a.pow(999), DECIMAL128)`: **8 ms** (the worst operand); 6 ms for `1.0…01` (100 digits)
  - On a cold JVM the exact power took 630 ms once, and the negative path 400 ms.
  - The worst operand written in plain notation is 1,002 characters, over the 1,000-character JSON string limit, so tests must send it in E notation (`9.99…9E-901`) to reach POWER at all.

## Goals / Non-Goals

**Goals:**
- POWER results are *correctly* rounded (HALF_EVEN on the exact value), matching DIVIDE's BR-3 guarantee.
- Every new rule lives in its `Operation` constant. Adding the operations touches only the enum, two exceptions, two handlers and the docs examples.

**Non-Goals:**
- No general "result limit" for ADD, SUBTRACT, MULTIPLY or DIVIDE. Their results are already bounded by BR-1 (about 2,100 digits at most).
- No generic domain-error hierarchy. There are three 422 exceptions now, but each still maps to its own `type`.

## Decisions

### D1. POWER = exact power, rounded once

```java
POWER {
  BigDecimal apply(BigDecimal a, BigDecimal b) {
    int n = exponent(b);                                   // BR-5, throws InvalidExponentException
    if (n < 0 && a.signum() == 0) throw new DivisionByZeroException();   // BR-2
    BigDecimal r = n >= 0 ? a.pow(n).round(DECIMAL128)
                          : BigDecimal.ONE.divide(a.pow(-n), DECIMAL128);
    if (Math.abs((long) r.stripTrailingZeros().scale()) > 1000) throw new ResultOutOfRangeException(); // BR-6
    return r;
  }
}
```

- The exact power has at most 100 × 999 = 99,900 digits, with |scale| ≤ 999,000, which fits in an `int`. One `round` or `divide` with `DECIMAL128` then gives correctly rounded HALF_EVEN, the same contract as DIVIDE.
- `a.pow(0)` is `1` for every `a`, including `0`, as the spec requires.
- `(long)` on the scale is for the same reason as in BR-1: avoid `Math.abs(Integer.MIN_VALUE)`.

*Alternative:* `a.pow(n, MathContext(44))`, then round to 34. It's about 100× faster and matched the exact method on 2,000 random cases. **Rejected**: Java only guarantees "within two ulps", so a near-halfway case could round the wrong way, and BR-3 promises HALF_EVEN. 7 ms of worst-case CPU is fine at this API's load. If load ever matters, this is the lever, and it would get a `shortcut:` note.

### D2. Exponent check (BR-5)

`exponent(b)`: `b.stripTrailingZeros().scale() > 0` means it isn't an integer. `b.abs() > 999` means it's out of range. Either way it throws `InvalidExponentException`; otherwise it returns `b.intValueExact()`. `"3.0"` and `"3E+0"` count as 3. `"1E+3"` is 1000, so it's rejected. The check runs before the zero-base check, which gives the spec's order: `0^-0.5` → invalid-exponent.

### D3. MODULO = `BigDecimal.remainder`

`if (b.signum() == 0) throw new DivisionByZeroException(); return a.remainder(b);`

`remainder` is the truncated remainder `a − b × trunc(a ÷ b)`, which is exact and takes the sign of `a`. These match the spec scenarios, checked in jshell and with Python's `decimal %`: `−7 mod 3 = −1`, `7 mod −3 = 1`, `5.5 mod 2 = 1.5`, `−7.5 mod 2 = −1.5`, `1E+1000 mod 7 = 4`.

### D4. Two domain exceptions, two handlers

`InvalidExponentException` and `ResultOutOfRangeException` are plain `RuntimeException`s in `domain`, carrying no values. `ProblemDetailsAdvice` maps them to 422:
- `/problems/invalid-exponent`, title "Invalid exponent", detail "POWER needs an integer exponent between -999 and 999."
- `/problems/result-out-of-range`, title "Result out of range", detail "The result's scale must be between -1000 and 1000."

Neither echoes a value.

### D5. OpenAPI: examples follow the enum, with a test

- The 422 `@ApiResponse` gains two `@ExampleObject`s.
- `ProblemExamples.UNKNOWN_OPERATION` is updated to the six names.
- **v2 audit fix:** `ApiDocumentationTest` parses the example's `detail`, extracts the list after "Supported operations (case-sensitive): ", and asserts it equals `Operation.values()` joined. The next Operation without an example update then fails the build. The values must be compile-time constants because they're used in annotations, so the test is the guard, not generation.

### D6. Docs

- **DOMAIN-MODEL:**
  - BR-2 widens to MODULO and zero to a negative power.
  - BR-3 adds POWER's rounding.
  - New rules: BR-5 (exponent), BR-6 (POWER result bound), BR-7 (MODULO truncated, sign of `a`).
  - §3 gets two rows, with every edge case from the spec.
- **PRD:**
  - §3.1 rows.
  - §3.2 error table: two 422 rows.
  - The validation order line.
  - §6: the "more binary operators" roadmap row marked delivered.
  - Change log.

## Risks / Trade-offs

- [7 ms worst-case CPU per POWER request, 630 ms once on a cold JVM] → Bounded by BR-1 and BR-5, and covered by a 1-second test. No auth or rate limiting yet (PRD non-goal), so a flood of worst-case POWER requests costs about 140 req/s per core. Accepted, since the PRD already lists rate limiting as a roadmap item.
- [Clients expecting floored modulo] → The rule is documented in BR-7, the PRD and the OpenAPI description; `−7 mod 3 = −1` is a spec scenario.
- [`stripTrailingZeros` before the bound check differs from what BR-1 checks (written form)] → Deliberate. BR-1 is about input as written, and BR-6 is about the Result as rendered, which is always normalised.

## Open Questions

None. The exponent, result-bound and modulo-sign rules were decided by the user before this proposal.
