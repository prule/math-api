# Retrospective: add-operations-discovery

## Caught by review

- **Plan review (ponytail): HEAD left optional in a SHALL.** The spec said methods "other than `GET` (or `HEAD`)" get 405, so it was unclear whether a HEAD returning 200 met it. The requirement now says the service serves `GET` and `HEAD` and every other method gets 405. `headIsServed` pins it.
- **Code review (ponytail):** no findings.
- **Code review (`/code-review`, standards): a test pinned the whole Operation list.** `entriesHoldExactlyNameAndIntegerArity` compared all eight entries strictly, so adding an Operation would have broken it, against STANDARDS §3's "nothing else should need to change". It is replaced by `binaryAndUnaryEntriesGiveNameAndIntegerArity`, which uses `contains` for the ADD, SQRT and NEGATE entries the spec names. Exact keys, integer `arity` and order stay pinned by the enum-derived `listsEverySupportedOperationInOrderWithItsArity`. A BR-9 citation was added.
- **Code review (`/code-review`, spec): order vs the unknown-operation detail was untested.** The spec ties the list's order to the `unknown-operation` detail, but the tests only compared with `Operation.values()`. `orderMatchesTheUnknownOperationDetail` now reads the real detail. Reversing the controller's list makes both order tests fail, which was checked by a temporary mutation.
- **Skipped:** a test that query parameters are ignored, because the controller declares none and so cannot fail it. Also skipped `OperationResponse.from(Operation)` (Feature Envy, judgement call), because `CalculationController` maps inline the same way.

## Surprises

- **`jsonPath("$[0]").value(Map.of(...))` failed** on output that was correct. Cause unknown: probably JsonPath's map type vs `Map.of` equality in Spring's matcher. The line was redundant with the strict JSON compare and was dropped.
- **A doc-edit script failed its own guard** because the PRD change log reads "(both 400)", not "Both are 400". No file was written, but a `sed` chained after it with `;` still ticked tasks 2.x early. The edits were redone before the tasks counted as done.

## Shortcuts taken

None.

## Lessons

- **Chain a task-checkbox `sed` with `&&` after the step it records, never with `;`**, so a failed step can't mark its task done (apply step, `tasks.md`).
- **When a spec scenario lists every Operation by name, the test should still compare with `Operation.values()` and pin only the named examples with `contains`.** `ApiDocumentationTest` already did this. Propose should write task 1.x that way so the code review doesn't have to catch it (propose step, `tasks.md`).
- **When a spec says "same order as X", the task should test against X itself**, not against the shared source both read (propose step, `tasks.md`).
