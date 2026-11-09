MVP · backlog items 1–5 · 9 Nov 2026

# Math API: two builds compared

The same PRD, domain model, standards and backlog, built twice. The original `prule/math-api` used one agent working through OpenSpec. The rebuild `prule/math-api-sdlc-pipeline` used the sdlc-pipeline: five specialist agents and two human gates. This page compares the two up to the end of the MVP (original items 1–5, rebuild B-1–B-5).

Original: single agentRebuild: multi-agent pipeline

## The short version

Active time, items 1–5

Original75 min

Rebuild133 min

1.8× longer

Output tokens, items 1–5

Original217k

Rebuild562k

2.6× more

Review findings recorded

Original0

Rebuild36

The original had no review step until after v1

Tests at end of MVP

Original108

Rebuild155

Both green; the rebuild's are mutation-checked

The pipeline cost about twice the time and two and a half times the output tokens. In return it gave five planned and reviewed changes, five retrospectives, a ledger of lessons, and tests proven to catch broken code. The two codebases ended up very close: the same architecture, the same request limits and the same domain shape. Each is better than the other on a few specific behaviours (see [Code](#code)).

## Two methodologies

|  | Original (items 1–5) | Rebuild (B-1–B-5) |
| --- | --- | --- |
| Who does the work | One agent, in the main session | An orchestrator plus five agents: architect, spec-reviewer, junior-dev, qa, senior-dev |
| Steps per change | `/opsx:propose` → `/opsx:apply` → archive → PR | Use case → plan → spec review → **Gate 1** → implement → QA (`opsx:verify`) → code review → **Gate 2** → archive → retrospective → PR |
| Input per change | An implementation-level OpenSpec prompt from `BACKLOG.md` | A business-level use case (`use-cases/UC-00n`), with no implementation detail |
| Reviews | None in v1. The ponytail plan and code reviews were added at PR #7, and `/code-review` at PR #12. | Three agent gates per change (plan, QA, code), plus two human gates |
| Learning loop | Retrospectives started at item 6 | A retrospective every change, plus `LEDGER.md` (19 open lessons after B-5) |
| Human touchpoints | 2 questions in total (in setup) | 11 decisions: 2 per change, plus one budget check on B-4 |
| Starting docs | Grew as the build went (PRD, then the domain model, then standards) | The original's *final* PRD, domain model and standards |

## Effort per item

The figures come from the two session logs, split at each `gh pr merge`. **Active time** counts the time between log events, ignoring any gap over 3 minutes, which removes waits for a human. Item 1 in the original also includes project setup and some PRD grilling. The rebuild's setup is shown separately and left out of the totals.

### Active minutes

Per backlog item

### Output tokens (thousands)

Per backlog item, all agents included

| Item | Active min orig / rebuild | Output tok orig / rebuild | Cache read orig / rebuild | Cache write orig / rebuild | Tool calls orig / rebuild | Agent runs |
| --- | --- | --- | --- | --- | --- | --- |

Cache writes grew the most, about 7×. Every agent starts cold and re-reads the profile, the docs and the code, so the cost of context is paid five or six times per change instead of once. Cache reads grew less, 1.4×, because the original's single long session re-read its own growing history on every turn.

## Where the rebuild's agent time went

| Agent | Runs | Minutes | Output tokens | Role |
| --- | --- | --- | --- | --- |
| architect | 12 | 32.9 | 390k | Plans, plus every fix round (resumed) |
| junior-dev | 6 | 20.3 | 238k | Implements `tasks.md` |
| qa | 5 | 16.7 | 155k | `opsx:verify`, plus mutation runs on a scratch copy |
| spec-reviewer | 6 | 9.0 | 100k | Plan gate |
| senior-dev | 5 | 6.3 | 60k | Code review gate |
| **All agents** | 34 | 85.2 | 943k | Across setup, B-1–B-5 and the audit |

Planning took the largest share. The architect used about 40% of agent time, partly because the use cases left implementation detail to it. The original's backlog prompts already named classes and libraries. QA took a fifth, mostly on breaking the code on purpose to prove the tests catch it. The orchestrator itself spent about 155 minutes in the session: sequencing, relaying and bookkeeping.

## Reviews and retrospectives

### Findings per change (rebuild)

| Change | Std. viol. | Other defects | Failed reviews | Lessons |
| --- | --- | --- | --- | --- |
| B-1 health + skeleton | 1 | 3 | 0 | 3 |
| B-2 ADD/SUB/MUL | 2 | 7 | 0 | 6 |
| B-3 DIVIDE | 0 | 3 | 0 | 1 |
| B-4 operand limits | 3 | 9 | 3 | 7 |
| B-5 API docs | 3 | 5 | 0 | 6 |
| **Total** | 9 | 27 | 3 | 23 |

The original recorded no review findings for items 1–5, because it had no review step. Its later retrospectives (items 6–8) list catches made by ponytail review once that step existed.

### Catches that mattered

- **Framework errors with no `type`** (B-2). Spring 7 leaves `ProblemDetail.type` empty, so a 405 or 415 had no `type` field. The defaults table the architect wrote, a lesson the ledger carried from B-1, made the junior dev check it and fix it.
- **A wrong boundary value in the plan** (B-4). `98765E+1000` was labelled out of range, but it sits exactly on BR-1's limit. The junior dev stopped instead of weakening the test.
- **A disproved risk claim** (B-4). The spec reviewer ran Jackson itself and showed the architect's example was wrong before you were asked to accept it.
- **Two tests that couldn't fail** (B-4). A `Math.abs` scale check and an echo of the value as `9.8765E+1005` both passed every test. QA found and closed both by breaking the code on purpose.
- **Docs that overclaimed** (B-5). The API description said every other HTTP error was `about:blank`. An unhandled 500 is not.

The ledger is not closing its loop yet. Lessons L-1, L-3, L-6 and L-11 each recurred, and four lines are at 2 records. But they "held" only because the orchestrator pasted them into each agent's prompt. None of their targets has been changed: STANDARDS, the profile, `openspec/config.yaml` and the plugin's agent files are all untouched.

## Code

|  | Original @ `aea85a0` | Rebuild @ `8a8e3b0` |
| --- | --- | --- |
| Production Java (lines / files) | 470 / 18 | 473 / 13 |
| Test Java (lines / files) | 817 / 9 | 1,390 / 8 |
| Tests run (all green) | 108 | 155 |
| Dependencies (main / test) | 4 / 5 | 4 / 2 |
| Planning artifacts (lines, 5 changes) | 1,136 | 1,708 |

Both builds reached the same shape on their own. Both have a `domain / application / adapter.in.web / config` split enforced by ArchUnit, an `Operation` enum with an exact `named()` lookup, BR-1 checked in the `Calculation` constructor, one `ResponseEntityExceptionHandler` advice, and **the same three Jackson read limits** (16,384 / 1,000 / 1,000). The original kept two test dependencies it never used (removed later in its 5b); the rebuild never added them.

### Behaviour, checked by running both jars

| Case | Original | Rebuild | Better |
| --- | --- | --- | --- |
| 415 / 405 framework error | Problem Details **with no `type`**. PRD §3.2 requires one. | `type: about:blank`, with tests | Rebuild |
| Docs examples | Hand-written JSON constants (`ProblemExamples`). Its later audit flagged the hand-listed Operations. | Generated from the domain. Each example is compared with a live response. | Rebuild |
| Tests proven to catch broken code | Not checked | QA ran 8–21 mutants per change. Every gap found was closed. | Rebuild |
| Deeply nested 1.2 KB body | Generic `invalid-request` detail | Says "too large: at most 16,384 bytes", which is wrong (audit finding, now B-13) | Original |
| Several required fields missing | Names every missing field, sorted | Names one; which one isn't fixed (waived) | Original |
| Operand out of range | Says which limit was broken (digits or scale) | States both limits | Original |
| Unknown operation | Echoes the given name, e.g. `'add'` | Doesn't echo it | Choice |
| `/actuator/health` | `{"groups":[…],"status":"UP"}`, with exposure pinned to health | `{"status":"UP"}` only. Follows readiness: `503 OUT_OF_SERVICE`, tested. | Choice |
| BR-1 at `Integer.MIN_VALUE` scale | `Math.abs((long) scale)` | Two direct comparisons | Both correct |

## What makes the comparison unfair

- **Better starting docs.** The rebuild started from the original's final PRD, domain model and standards. Several hard decisions were already written down: the failure order, the limits, the pinned versions and the exception names.
- **Lessons injected by hand.** I added each ledger lesson to the agents' prompts myself. A pipeline run without that would likely repeat more mistakes.
- **Different review regimes.** The original v1 had no reviews, so its defect count of zero means "not looked for", not "none". By items 6–8 it had added ponytail and `/code-review`, which closed some of the gap.
- **One long session.** The rebuild's five runs share one session, so the orchestrator's later turns carry more context and cost more. Its token figures for the later items are slightly inflated.
- **Wall-clock time includes humans.** The rebuild's 313 wall minutes against the original's 85 include about 70 minutes of waiting at gates. The active-time figures leave that out.

## Takeaways

- **The pipeline buys verification, not a different design.** Both builds converged on the same structure. The gain is evidence: specs that match the code, tests proven by mutation, and a recorded reason for each deviation.
- **The cost goes on cold context and planning.** Cache writes rose 7× and the architect took 40% of agent time. Giving use cases more technical context, or merging the plan review into the plan step for small changes like B-3, would cut both.
- **The gates paid off on the hardest change.** B-4 had three failed reviews and 15 findings. The easy change, B-3, came through clean. That suggests a lighter route for small changes.
- **Both builds miss some error details.** Each has defects the other avoided. A shared catch-all 500 handler and fuller error details would make both stronger (ledger L-17, backlog B-13).
- **Adopt the recurring lessons.** Writing L-1, L-3, L-6 and L-11 into STANDARDS, the profile and `openspec/config.yaml` would make the pipeline improve itself without the orchestrator having to remember.

Sources: session logs `1bf91e3b…` (original) and `47ef3287…` (rebuild, including 75 subagent transcripts), split at each PR merge; `reports/sessions/*.summary.json`; `retrospectives/`; the test-result XML from both builds; and live probes of both jars on local ports. The original was measured at `aea85a0` (v1 complete) and the rebuild at `8a8e3b0` (MVP complete).