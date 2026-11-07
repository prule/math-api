# Design

## Context

The skeleton from `add-walking-skeleton` provides empty layer packages, `ArchitectureTest` (with an "empty layers allowed" shortcut), a MockMvc smoke test and actuator config. Spring Boot 4.1.2 ships **Jackson 3** (`tools.jackson.*`) as its JSON mapper. The four layers and their allowed dependencies are fixed by STANDARDS.md §2, and the domain terms by DOMAIN-MODEL.md §1.

## Goals / Non-Goals

**Goals:**
- Each concern lives in exactly one place:
  - arithmetic in `Operation`
  - normalisation (BR-4 stripping) in `Calculation`
  - plain-notation formatting and HTTP mapping in the web adapter
- Adding `DIVIDE` in item 3 should need only a new constant, a domain exception and one handler method. This design is judged by that.

**Non-Goals:**
- No port/adapter for persistence, no mapper library, no generic "operation registry".
- No custom Jackson configuration beyond what the scenarios need.

## Decisions

### D1. `Operation` is an enum whose constants implement `apply`

```java
public enum Operation {
    ADD { public BigDecimal apply(BigDecimal a, BigDecimal b) { return a.add(b); } },
    SUBTRACT { ... a.subtract(b) ... },
    MULTIPLY { ... a.multiply(b) ... };
    public abstract BigDecimal apply(BigDecimal a, BigDecimal b);
    public static Operation named(String name) { ... }  // throws UnknownOperationException
}
```
`BigDecimal.add/subtract/multiply` without a `MathContext` are exact, which satisfies BR-3 with no extra code. `named` wraps `valueOf`, which is exactly the case-sensitive match the spec asks for, and turns its `IllegalArgumentException` into the domain's `UnknownOperationException` (carrying the rejected name).

*Alternative:* let Jackson bind straight to the enum. Rejected, because a bad name then becomes a generic deserialisation error that can't be told apart from malformed JSON, and the spec needs two different `type`s.

### D2. `Calculation` record owns normalisation

`record Calculation(Operation operation, BigDecimal a, BigDecimal b)` rejects nulls in its compact constructor, and `result()` returns `operation.apply(a, b).stripTrailingZeros()`. Since Java 8, `stripTrailingZeros()` maps every zero to `0` (scale 0), which covers BR-4's "zero is `0`". Plain notation is a *rendering* concern, so the web adapter applies `toPlainString()`. That one call is what keeps `1E+6` out of responses.

### D3. `PerformCalculation` use case is a plain class

`BigDecimal perform(Calculation calculation)` returns `calculation.result()`. It's thin today on purpose: it's the seam where history (backlog item 10) or other cross-cutting work will attach, and STANDARDS.md routes web → application → domain. It has no Spring annotations and is wired as a `@Bean` in `config.UseCaseConfig`.

### D4. Web DTOs are records with bean validation

- `CalculationRequest(@NotNull String operation, @NotNull BigDecimal a, @NotNull BigDecimal b)`
- `CalculationResponse(String operation, String a, String b, String result)`

Binding JSON straight to `BigDecimal` makes Jackson read a JSON number from its text, so it's exact (`0.1` stays `0.1`), and it coerces a JSON string such as `"2.5"` by default. That covers "numbers or strings" with no custom deserialiser. `operation` is a `String` so that D1's lookup decides unknown-operation. The controller maps request → `Calculation` (via `Operation.named`) → use case → response, with `toPlainString()` on `a`, `b` and the Result.

### D5. One `@RestControllerAdvice` extending `ResponseEntityExceptionHandler`

- Override `handleHttpMessageNotReadable` (malformed JSON, non-numeric operand) and `handleMethodArgumentNotValid` (missing/null field) to return `ProblemDetail` with type `/problems/invalid-request`, title "Invalid request", and a fixed, human-readable `detail`. For validation errors, the detail names the offending fields. Never pass through the Jackson message, which holds class names.
- Add an `@ExceptionHandler(UnknownOperationException.class)` that returns 400 with type `/problems/unknown-operation`, and a detail naming the rejected operation and the supported names (`Operation.values()`).
- Extending `ResponseEntityExceptionHandler` means framework errors (405, 415, 406) also come back as Problem Details, which is what the spec's "every error response" requirement needs. `ProblemDetail` responses are served as `application/problem+json` by default.
- Type URIs are relative (`URI.create("/problems/invalid-request")`), as listed in the PRD.

*Alternative:* `spring.mvc.problemdetails.enabled=true` alone. Rejected, because it can't set our `type` values or clean the `detail`.

### D6. Tests by layer

| Test | Kind | Covers |
|------|------|--------|
| `OperationTest` | plain JUnit + AssertJ | every DOMAIN-MODEL §3 edge case for ADD/SUBTRACT/MULTIPLY; `named` exact and case-sensitive |
| `CalculationTest` | plain JUnit | BR-4 normalisation (trailing zeros, zero → `0`), null rejection |
| `PerformCalculationTest` | plain JUnit | delegates to `Calculation.result()` |
| `CalculationControllerTest` | `@WebMvcTest(CalculationController.class)` + `@Import(UseCaseConfig.class)` | request/response contract, number/string operands, echo in plain notation, every Problem Details path and its shape |
| `ServiceHealthSmokeTest` | existing `@SpringBootTest` | plus one happy-path POST |

The web slice imports the **real** use case config rather than mocking. The use case is pure and cheap, and mocking it would hide wiring errors.

### D7. Remove the ArchUnit shortcut

Each layer now has a class (`Operation`, `PerformCalculation`, `CalculationController`, `UseCaseConfig`), so remove `allowEmptyShould(true)` and `withOptionalLayers(true)` from `ArchitectureTest`, along with the `shortcut:` comment. The layered rule already lets `Web` access `Domain`, which the controller and advice need for `Operation`, `Calculation` and `UnknownOperationException`.

## Risks / Trade-offs

- [Jackson 3 coercion defaults differ from Jackson 2] → The string-operand and `0.1 + 0.2` web tests fail if string → `BigDecimal` coercion or exact number reading isn't the default. In that case, enable the needed feature in a `JsonMapperBuilderCustomizer` in `config` and record it here.
- [`detail` messages hand-written in the advice could drift from what failed] → Keep them generic but accurate. Validation details list field names from `BindingResult`, not free text.
- [Echoing `a`/`b` with their given scale (`"1.50"`) while the Result is stripped (`"1"`) could look inconsistent] → This is deliberate. The echo shows what was received, and BR-4 applies to Results only. It's stated in the spec and in PRD §3.2.
- [No BR-1 limits yet: `1e999999999`-style operands are accepted] → Known and planned for backlog item 4. Don't expose the service publicly before item 4.

## Open Questions

None.
