# Retrospective: add-power-modulo

Backlog item 6, the first change of v2. It ran under the CLAUDE.md change workflow, preceded by the milestone `/ponytail:ponytail-audit`.

## Caught by review

- **Milestone audit (before the change):** the unknown-operation OpenAPI example listed the four operations by hand, so it would go stale as soon as POWER and MODULO landed. Fixed in this change: the example lists six names, and `ApiDocumentationTest.unknownOperationExampleListsTheSupportedOperations` compares it with `Operation.values()`. It failed on the stale list before the update.
- **Plan review:** the "Worst-case operand stays fast" scenario could pass without POWER running. Written in full, the worst BR-1 operand is 1,002 characters, over the 1,000-character JSON string limit, so it would get a fast 400 `invalid-request`. The scenario also covered only `b = 999`. Fixed: the operand is sent in E notation (`9.99…9E-901`); the scenario asserts the real outcomes (`^999` and `^−999` → 422 `result-out-of-range`; `1.0…01^999` → 200 `"1"`), each within 1 s; and the design records the measured 8 ms negative-exponent cost.
- **Code review:** `Operation.MAX_RESULT_SCALE = 1000` was a second copy of BR-1's scale bound, which BR-6 is defined to reuse. Fixed: POWER compares against `Calculation.MAX_SCALE`.

## Surprises

- **`OperationTest.namedRejectsUnsupportedOperation` used `"POWER"` as its unknown name** and failed once POWER existed. Cause: `add-divide`'s review swapped the planned names out of the web test only, and this change's plan review searched the tests for hard-coded operation lists, not for planned names used as "unknown" examples. Fixed by using `NOT_AN_OPERATION`.
- **Cold vs warm cost of the exact power:** 630 ms on a cold JVM vs 7 ms warm (jshell). The 1-second tests pass in the warmed test JVM. A deployed app's first worst-case POWER request after start-up could be slow. Cause: JIT warm-up; not measured against a freshly started app.
- **First POWER draft dropped the `(long)` cast that design D1 names.** It's harmless, because exact-power scales stay within ±1,100,000, but I aligned it with the design before review.

## Shortcuts taken

None. `/ponytail:ponytail-debt` found no `shortcut:` markers in the repo.

## Lessons

- **Step 2, plan review:** when a change adds an Operation, grep the tests for its name used as an unknown example (`git grep -n '"POWER"' src/test`), not just for hard-coded lists.
- **Step 1, propose:** a timing scenario must also state the expected outcome, and its inputs must fit the request limits (1,000-character tokens, 16,384-character body). Otherwise the test can pass on an early rejection.
- **Backlog prompts for "Later" items:** naming the open decisions in the prompt (exponent range, rounding, modulo sign) let propose ask them in one round of three questions. Keep doing this for items 7–11.
- **Milestone audit:** doing it before the first v2 change let one finding ride along in this change instead of becoming its own PR.
