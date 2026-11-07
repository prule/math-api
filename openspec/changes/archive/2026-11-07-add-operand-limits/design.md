# Design

## Context

- `Calculation` is constructed in `CalculationController` straight after `Operation.named(...)`, and `result()` runs only afterwards in `PerformCalculation`. So a check in `Calculation`'s compact constructor runs before any arithmetic by construction.
- Jackson 3.1.7 (`tools.jackson`) is the JSON reader. Its `StreamReadConstraints` defaults are: max number length **1,000**, max string length **100,000,000**, max document length **unlimited**, max nesting depth 500.
- Spring Boot 4.1.2 exposes these limits as `spring.jackson.factory.constraints.read.*` properties (`max-document-length`, `max-string-length`, `max-number-length`, …), so no Jackson code is needed.

## Goals / Non-Goals

**Goals:**
- Bound the cost of the worst request: every number the arithmetic sees has ≤ 100 digits and |scale| ≤ 1000, and every token Jackson parses has ≤ 1,000 characters.
- Keep each rule in exactly one place: BR-1 in the domain, parse limits in config.

**Non-Goals:**
- No `Operand` value type. The glossary already defines Operand as "a `BigDecimal` within BR-1", and a wrapper type would touch every layer for no behavioural gain. Revisit if Operands gain more rules.
- No servlet filter or `Content-Length` check. Jackson's document limit bounds what is actually read, including chunked bodies that carry no `Content-Length`.

## Decisions

### D1. Check BR-1 on the Operand *as written*, not on its normalised value

`precision() ≤ 100 && scale() >= -1000 && scale() <= 1000` on the received `BigDecimal`.

*Alternative considered:* check the value after `stripTrailingZeros()`, so that `1` followed by 100 zeros (value 1e100) would be accepted. **Rejected for safety:** `0E-999999999` strips to `0` and passes, but the controller echoes `a`/`b` with their given scale. `toPlainString()` would then build a billion-character string, and the arithmetic would also run on the raw scale. Checking as written bounds both the echo (≤ ~1,100 characters) and the arithmetic. Worst cases: `ADD` gives ~2,100 digits, `MULTIPLY` gives precision 200 with |scale| ≤ 2,000, and `DIVIDE` gives 34 digits.

**Consequence:** BR-1's note "1e1000 is the largest power of ten accepted" is inaccurate. `100E+999` (value 1e1001) passes, and a 101-digit integer is rejected even though its value is small. DOMAIN-MODEL.md is corrected to state the rule as written.

### D2. `Calculation` constructor enforces BR-1, then `OperandOutOfRangeException`

```java
public Calculation {
    requireNonNull(...);              // existing
    requireWithinLimits("a", a);      // BR-1
    requireWithinLimits("b", b);
}
```
- `OperandOutOfRangeException(String operand, Limit limit)` uses a small nested `enum Limit { DIGITS, SCALE }`. The handler can build a precise detail without the domain knowing about HTTP, and without the value ever reaching the message. That rules out log/echo amplification and satisfies "does not echo the value".
- The limits are `private static final` constants in `Calculation` (`MAX_DIGITS = 100`, `MAX_SCALE = 1000`). They're not configurable (out of scope).
- `a` is checked before `b`, so if both are out of range, `a` is reported. That's deterministic, and the spec only requires that the named operand is one that broke a limit.

### D3. Validation order falls out of the existing flow

The controller calls `Operation.named` (unknown-operation), then `new Calculation` (BR-1), then `perform` (BR-2 inside `DIVIDE.apply`). Jackson/validation failures happen before the controller runs. No reordering code is needed, and the spec's order scenarios pin it in tests.

### D4. JSON read limits in `application.yaml`

```yaml
spring:
  jackson:
    factory:
      constraints:
        read:
          max-document-length: 16384
          max-string-length: 1000
          max-number-length: 1000
```
- **16,384 characters:** the largest valid request is roughly 2 × 1,100-character operands plus keys, so this leaves about 7× headroom while capping per-request parse work.
- **1,000-character strings:** operation names and operands fit, and a 100-million-digit string can no longer reach `new BigDecimal(String)`, whose parse cost grows super-linearly.
- **Number length:** stated explicitly although it equals the default, so a future Jackson default change can't loosen it silently.
- Exceeding a limit throws `StreamConstraintsException` (a `JacksonException`). Spring's Jackson converter wraps it in `HttpMessageNotReadableException`, and the existing advice already maps that to `400 invalid-request` with a fixed detail. No new web code is needed for this part.
- These properties apply to every Jackson parser in the app, which is fine: the calculation request is the only JSON input.
- *Found during implementation:* Jackson 3.1.7 already applies `max-number-length` when coercing a JSON **string** to `BigDecimal`, so a 1,001-digit string operand was rejected even before this config. `max-string-length` is still needed for non-numeric strings (e.g. a 1,001-character `operation`), and a dedicated test proves it's effective. Both properties worked as named, so neither fallback in Risks was needed.
- *Found in review:* `new BigDecimal("1E+2147483648")` parses to scale `Integer.MIN_VALUE`, and `Math.abs(int)` of that is still negative. That's why D2's scale check widens to `long`, and the comment and a `CalculationTest` case pin it (verified by reverting the cast: the test fails). Through the API, Jackson's own parser already rejects such exponents (number or string) as `invalid-request`, so the domain guard is defence in depth. Web tests pin the parse-time rejection.

### D5. One new handler in `ProblemDetailsAdvice`

`@ExceptionHandler(OperandOutOfRangeException.class)` returns 400 with type `/problems/operand-out-of-range`, title "Operand out of range", and a detail such as "Operand 'b' must have a scale between -1000 and 1000 (at most 1000 decimal places)." or "Operand 'a' must have at most 100 significant digits."

### D6. Tests

- `CalculationTest` covers:
  - limits exactly met: a 100-digit integer, `1E+1000`, `1E-1000`
  - one past each limit, for both `a` and `b`, asserting `operand()` and `limit()`
  - `1e999999999` rejected inside `assertTimeoutPreemptively(Duration.ofSeconds(1), …)`
- `CalculationControllerTest` has one test per spec scenario:
  - the boundary cases, through the API
  - the extreme-exponent requests under a 1-second `assertTimeoutPreemptively`
  - oversized body, string and number (built with `"9".repeat(1001)`, plus padding of 17,000 characters)
  - a 150-digit string (operand-out-of-range)
  - both order scenarios
  - every error goes through the existing `expectProblem` shape check, plus "detail names the operand and doesn't contain the value"
- `@WebMvcTest` loads `application.yaml`, so the Jackson constraints apply in the slice. Task 3.1 confirms this, since the oversized tests fail without the config.

## Risks / Trade-offs

- [The property names under `spring.jackson.factory.constraints.read` could be ignored] → The oversized-input tests fail first (task 3.1). If they still fail after config, fall back to a `JsonFactoryBuilderCustomizer` bean in `config` that calls `streamReadConstraints(...)`, and record it here.
- [Spring doesn't wrap `StreamConstraintsException` as `HttpMessageNotReadableException`, so a 500 results] → The same tests catch it. Fall back to mapping it explicitly in the advice.
- [Rejecting `1` followed by 100 zeros surprises a client] → It's documented in BR-1 and the detail says "significant digits". The workaround (`1E+100`) is given in DOMAIN-MODEL.md.
- [A 16 KB document limit blocks future larger requests, e.g. a batch API] → That's a future change, which would raise the limit deliberately.

## Open Questions

None.
