# Design

## Context

- **Versions:** springdoc-openapi 3.2.0 is the current release, and its parent POM is `spring-boot-starter-parent:4.1.2`, our exact Boot version. It uses swagger-core 2.2.55.
- **Request DTO:** `CalculationRequest(String operation, BigDecimal a, BigDecimal b)` with `@NotNull`. Without hints, springdoc would document `a`/`b` as `number` only and `operation` as a free string.
- **Errors:** they come from `ProblemDetailsAdvice`, which springdoc can't link to the operation without explicit `@ApiResponse`s.
- **Name clash:** the domain `Operation` enum clashes with swagger's `io.swagger.v3.oas.annotations.Operation` annotation in the controller.

## Goals / Non-Goals

**Goals:**
- Adding an Operation still means changing only the enum (DOMAIN-MODEL §2). The documented allowed values must follow automatically.
- The generated document is checked by a test, so docs can't silently drift from the contract.

**Non-Goals:**
- No hand-maintained YAML spec, and no build-time spec generation plugin.
- No docs for actuator (`springdoc.show-actuator` stays false).

## Decisions

### D1. springdoc 3.2.0, OpenAPI 3.1

Add `implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.2.0")`. Set `springdoc.api-docs.version: openapi-3-1` explicitly. That's the value springdoc 3.2.0's own settings list, and its default, but stating it pins the number-or-string type array (D3) to a version where it's valid OpenAPI, regardless of future defaults. The paths stay at springdoc's defaults (`/v3/api-docs`, `/swagger-ui.html`), but are stated in `application.yaml` for visibility.

*Alternative:* springdoc 3.0.x. Rejected, because 3.2.0 targets our Boot minor version.

### D2. Allowed operations come from the enum through an `OpenApiCustomizer`

`config.OpenApiConfig` defines:
- an `OpenAPI` bean with the title ("Math API"), version and a one-line description
- an `OpenApiCustomizer` that sets `components.schemas.CalculationRequest.properties.operation.enum` to `Arrays.stream(Operation.values()).map(Enum::name)`

*Alternative:* `@Schema(allowableValues = {"ADD", …})` on the DTO. Rejected, because a new Operation would then need an annotation edit in the web layer, which breaks "adding an operation = one constant". A test could catch the drift, but the customiser makes drift impossible. It's about 10 lines, and the `config` layer may depend on every layer.

### D3. Annotate DTOs for the parts springdoc can't infer

- On `CalculationRequest.a`/`b`: `@Schema(types = {"number", "string"}, example = "0.1", description = "Decimal number; JSON number or string")`.
- On `operation`: `@Schema(description = "Operation name, case-sensitive", example = "ADD")`. The allowed values come from D2.
- `@NotNull` is already present, so springdoc marks the fields as required.
- On `CalculationResponse` fields: `@Schema(example = …)`. They're already `String`, so the types are right without annotations.

### D4. All three responses declared on the controller method

**200:** declare it explicitly as `@ApiResponse(responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(implementation = CalculationResponse.class)))`. springdoc's `default-produces-media-type` is unset and `@PostMapping` declares no `produces`, so otherwise springdoc documents the 200 under `*/*`.

*Alternative:* add `produces = APPLICATION_JSON_VALUE` to `@PostMapping`. Rejected, because it changes runtime content negotiation, with a risk to the `application/problem+json` error responses, just to fix documentation.

**Errors:** use `@ApiResponse(responseCode = "400", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ProblemDetail.class), examples = {…three @ExampleObject…}))` and the same for `"422"` with one example. Each example's `value` is a real response body in JSON, copied from the curl output of the earlier changes, so the docs show exactly what clients get. Use the annotation's fully qualified name (`@io.swagger.v3.oas.annotations.Operation(summary = …)`) to avoid clashing with the domain enum.

Examples live in one place: a `ProblemExamples` class of `static final String` constants in `adapter.in.web`, next to the advice whose output they describe.

### D4a. Keep springdoc's generic responses from leaking in

springdoc's `override-with-generic-response` (on by default) scans `@RestControllerAdvice` beans and can attach their handlers' responses to every operation. Ours inherits Spring's `ResponseEntityExceptionHandler` methods for 405, 406, 415 and others. Since the spec requires exactly `200`, `400` and `422`, the test pins that set (D5). If springdoc adds or merges anything else, set `springdoc.override-with-generic-response: false` and record what it added here. Framework errors (405, 406, 415) stay undocumented, which is consistent with the four error `type`s this API defines.

### D5. One `@SpringBootTest` + MockMvc test reads `/v3/api-docs`

`ApiDocumentationTest` fetches `/v3/api-docs` and asserts each spec scenario with `jsonPath`:
- the `openapi` version
- the path and the `post` operation
- that the 200 response is under `application/json` (and not `*/*`), with string properties
- that the response codes are exactly `200`, `400`, `422`
- that `a`/`b` allow number and string
- that the `required` list is correct
- that the `operation` enum equals `Operation.values()` (computed in the test, not hard-coded)
- the four example `type`s under 400/422 `application/problem+json`

It also checks `/swagger-ui.html` redirects to a page that returns 200. A full context is needed, because springdoc's endpoints aren't in the `@WebMvcTest` slice.

## Risks / Trade-offs

- [springdoc's OpenAPI 3.1 rendering of `types` differs from `type: [number, string]` (e.g. `oneOf`)] → The test asserts the *meaning* (both number and string allowed). During implementation, adjust the assertion to the actual shape and record the shape here.
- [Example bodies drift from the advice's real detail text] → Accepted for v1. The `type`s are tested and are the stable contract, while the detail text is human-readable. A later change could generate the examples from the advice.
- [The Swagger UI webjar adds static assets and a dependency footprint] → Accepted. It's what PRD §3.3 asked for, and it's served only from `/swagger-ui/**`.
- [`max-string-length: 1000` or the document limit affects springdoc] → No effect: springdoc *writes* JSON, and the read constraints apply to parsing only.

## Implementation notes

- springdoc 3.2.0 renders `@Schema(types = {"number", "string"})` as an OpenAPI 3.1 type array, `"type": ["number", "string"]`, not `oneOf`. The test's `allowedTypes` helper accepts either form.
- Before annotations, the 200 response was documented under `*/*` (confirmed). The explicit `@ApiResponse` fixed it.
- springdoc added **no** generic responses from `ProblemDetailsAdvice`: the operation documents exactly `200`, `400` and `422`. So `springdoc.override-with-generic-response` stays at its default, and the exact-codes test will catch it if that ever changes.
- Example `value`s are rendered as JSON objects (not strings).
- *Found in review:* springdoc documented Spring's `ProblemDetail` with an extra `properties` object (the Java map that holds extension fields), but real responses never contain that key. The `OpenApiCustomizer` removes it, and `ApiDocumentationTest` pins the schema to `type`, `title`, `status`, `detail`, `instance`.
- *Found in review:* swagger-core brings Jackson 2.21 onto the runtime classpath. Requests are still parsed by Jackson 3, where the read limits from `add-operand-limits` are configured. The existing oversized-input tests prove it, and would fail if Jackson 2 took over request parsing.
- springdoc exposes no actuator endpoints: `/actuator` still links only `self`, `health`, `health-path`.

## Open Questions

None.
