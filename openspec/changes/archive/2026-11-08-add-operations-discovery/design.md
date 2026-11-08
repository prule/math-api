# Design

## Context

`Operation` already carries everything the endpoint reports: `values()` in declaration order and `arity()`. Two places already read the enum directly from outside the domain: `ProblemDetailsAdvice` (the unknown-operation detail) and `OpenApiConfig` (allowed values, unary names). `ProblemDetailsAdvice` extends `ResponseEntityExceptionHandler`, so a 405 is already Problem Details for any path.

## Goals / Non-Goals

**Goals:** one read-only endpoint that cannot drift from the enum; documented per STANDARDS §4.

**Non-Goals:** a use case or port for listing; any change to `Operation` or the calculation contract.

## Decisions

1. **Controller reads `Operation.values()` directly; no `ListOperations` use case.** Listing a closed enum has no business rule to put in `application`, and STANDARDS §5 forbids speculative abstraction. The web layer may depend on the domain (`CalculationController` already calls `Operation.named`), so ArchUnit stays as it is. *Alternative:* a pass-through use case in `application` wired in `UseCaseConfig` — rejected, the v2 audit already flagged `PerformCalculation` as a pass-through kept only because item 10 gives it work.
2. **Body is a bare JSON array of `{ name, arity }`, not `{ "operations": [...] }`.** It is the smallest shape that answers the question, and the list is the whole resource. *Alternative:* a wrapper object for future fields (links, paging) — rejected: HATEOAS is a PRD non-goal, the list is a closed set of eight, and a new field would ship as a new version per PRD §3.4 anyway.
3. **`arity` is a JSON integer, not a string.** STANDARDS §4 makes numbers strings to avoid precision loss in decimal values; an arity of 1 or 2 has no precision to lose, and clients will compare it as a number. STANDARDS §4 is reworded to say the string rule covers decimal values (Operands, Results), so the rule and the endpoint agree.
4. **Order is declaration order** (`values()`), the same as the unknown-operation detail and the OpenAPI enum, so all three lists read alike. The spec pins it so a test can compare with `Operation.values()`.
5. **New `OperationController` at `/api/v1/operations`, response record `OperationResponse(String name, int arity)`.** A separate controller keeps each class to one resource path, matching `CalculationController`. The record lives in `adapter.in.web` per STANDARDS §2 (DTOs never leave the adapter). springdoc infers an array of `OperationResponse` from a `List<OperationResponse>` return type; an explicit `@ApiResponse(200, application/json)` with `@ArraySchema` fixes the `*/*` media type, as STANDARDS §4 requires.

## Risks / Trade-offs

- [Tests that assert the literal eight names break when an Operation is added] → the web-slice test compares against `Operation.values()` and only the spec's example scenario names constants; the existing api-docs tests already work the same way.
- [Bare array can't grow extra top-level fields without a version bump] → accepted (decision 2).
