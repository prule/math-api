# Retrospective: add-unary-operations

## Caught by review

**Plan review (ponytail, before apply):**
- **An existing test would break with no task covering it.** `CalculationTest` asserted that a null `b` throws `NullPointerException`, which becomes `ArityMismatchException`. Fixed: task 1.1 now replaces that assertion.
- **HALF_EVEN was untested for SQRT.** `√10` rounds up under HALF_UP too, so `new MathContext(34)` (HALF_UP) would have passed. Fixed: two 70-digit tie cases (exact 35-digit roots) in the spec, `OperationTest` and `CalculationControllerTest`, checked on JDK 25 first.
- **Task 4.1 invited hard-coding the eight operation names.** Fixed: the task says the allowed-values and unknown-example tests keep deriving from `Operation.values()`.
- **Rewording the malformed-body detail would leave `ProblemExamples.INVALID_REQUEST` stale.** Fixed: task 4.2 updates the example.
- **The design cited an unverified JDK bug id for `sqrt` rounding.** Fixed: replaced by "checked on JDK 25" plus a measured worst case (2,000 roots in 13 ms).

**Code review, ponytail:**
- **The README still said "two operands".** Fixed in `README.md:5`.
- **The main `calculation` spec's Purpose said "two Operands"**, and archiving never touches Purpose. Fixed directly in `openspec/specs/calculation/spec.md`.
- **The hand-copied OpenAPI examples had no test.** Fixed: `CalculationControllerTest.eachProblemExampleIsTheRealResponse` posts a request for each of the 7 examples and compares the whole body strictly. I proved it fails when an example drifts.
- **An awkwardly wrapped comment in SQRT.** Shortened.

**Code review, `/code-review` Standards axis:**
- **Hard: `b`'s description copied the unary names "SQRT, NEGATE"**, against STANDARDS §4 (domain values are generated, not copied). Fixed: `OpenApiConfig` builds the list from `Operation.arity()`. `ApiDocumentationTest` asserts the description names exactly the unary Operations from the enum, and fails when the generated list is wrong.
- **Hard: the DOMAIN-MODEL edge case "b given (BR-9)" for NEGATE had no test.** Fixed: `unaryOperationWithBIsRejected_BR9` now runs for both SQRT and NEGATE.
- **The STANDARDS change-log row cited §2 instead of §3.** Fixed. The Spec axis flagged it too.
- **Judgement, not acted on:**
  - A bare `int` arity is decoded in two places (`isUnary()` was suggested). Kept: the glossary defines arity as 1 or 2, and backlog item 8 will report it as a number.
  - Unary constants ignore `b` (Refused Bequest). Kept: DOMAIN-MODEL §2 documents this on purpose.
  - STANDARDS §3 ("nothing else should need to change") reads more strictly than DOMAIN-MODEL §2 for a new kind of failure. Not fixed here.
- **Style:** a fully qualified `java.util.Arrays` in `OperationTest`. Fixed with an import.

**Code review, `/code-review` Spec axis:** every scenario is implemented and tested. Only trivial scope creep: the class-level request description changed, and one test also asserts "SQRT" in the detail. No changes needed.

## Surprises

- **A wrong expectation in my own web test.** It expected `"-0"` to echo as `"-0"`, but BigDecimal has no negative zero, so it echoes `"0"` (binary operations do the same). Cause: I wrote the expectation from the request text, not from BigDecimal's behaviour. Moved the echo check into its own test using `-2.50`.
- **I wrote the docs code (`ProblemExamples`, `@ApiResponse`) before the task 4.1 tests**, breaking test-first order for that group. I confirmed the 4.1 tests fail against the old web and config code (3 failures) before keeping them. Cause: the web task 3.2 and the docs task 4.2 share the same files, and I edited them in one pass.
- **`git checkout -b … origin/main` set the branch to track `origin/main`.** I unset it before any push. Cause: passing a remote start point sets upstream by default.
- **Spotless wrapped a long SQRT comment onto two lines**, leaving the word "form." on a line of its own. Found by the code review.
- **The archive ran `openspec archive --yes`** (the CLI's deterministic merge) instead of an agent-driven sync. Afterwards I checked every added and modified requirement in the main specs, and both pass `openspec validate --type spec --strict`.

## Shortcuts taken

None.

## Lessons

- **Before writing a web-test expectation for an echoed Operand, run the value through `new BigDecimal(x).toPlainString()`**, not the request text. Applies to `CalculationControllerTest` and any future echo assertions.
- **When a task group's tests and code share files with an earlier group (here the web 3.x and docs 4.x groups), write all the failing tests for both groups before touching the shared files.** That keeps test-first order honest. It changes how `tasks.md` should group work in `/opsx:propose`.
- **When a proposal makes a field optional, the propose step should grep for prose copies of that field's rule outside `docs/`** (README, spec Purpose, the OpenAPI description). Two of this change's review findings were stale "two operands" text that no task listed.
- **Create the feature branch with `git switch -c feat/<change> --no-track origin/main`** to avoid the accidental upstream.
