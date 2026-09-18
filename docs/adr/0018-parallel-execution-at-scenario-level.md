# 18. Parallel execution: the suite was ready, the application was not

## Status

Accepted.

## Context

[ADR 0005](0005-defer-parallel-execution.md) deferred parallel execution to Phase 3, and
[ADR 0013](0013-scenario-owned-fixtures.md) built the suite for it: every scenario creates its
own customer and reads nothing another created, specifically so that concurrency would be a
configuration change rather than a rewrite.

This checkpoint tested that claim. The baseline was 14.55s for four web scenarios
single-threaded, of which about 5s is JVM and browser warm-up.

## What was measured

Scenario level, dynamic strategy, four threads. Twenty consecutive runs per suite, each run a
fresh container:

| configuration | passed | wall clock |
|---|---|---|
| web, scenario level, 4 threads | **1 of 20** | 15–24s |
| api, scenario level, 4 threads | **0 of 20** | 14–16s |
| web, single-threaded | 12 of 12 | 19–22s |
| api, single-threaded | 3 of 3 | 14–15s |

The thread-count curve never appears, because there is no thread count above one at which the
suite is green. Wall clock at each, three runs:

| threads | web wall clock | passed |
|---|---|---|
| 1 | 19–22s | 12/12 |
| 2 | 20–22s | 0/3 |
| 4 | 17–21s | 0/3 |
| 8 | 20–23s | 0/3 |

Concurrency bought nothing even ignoring correctness. Four scenarios of which one carries ~5s
of warm-up cannot go much below that floor, and the parallel runs are only "fast" because a
failing scenario stops early.

## The diagnosis

Four candidates were on the table. Three are ruled out by evidence, not by inspection.

**Not fixture collision.** No scenario ever read another's data. Across the twenty runs, every
assertion failure names the scenario's *own* customer, and the one case where a scenario saw an
unexpected account is described below. Cucumber ran four scenarios on four distinct pool
threads in all twenty runs, with no thread executing two scenarios.

**Not the driver.** Same evidence: zero thread reuse, so no `ThreadLocal` could have leaked one
scenario's browser into another. Every browser-level failure was downstream of a customer that
had never been created.

**It was the application.** ParaBank cannot create two rows at the same time. Reduced to curl,
with no Trident anywhere in it, against a stock container:

| operation | sequential | concurrent |
|---|---|---|
| register a customer | 8 of 8 succeed | 50% fail at concurrency 2, 55% at 4, 47.5% at 8 |
| open an account over REST | succeeds | 6 of 8 fail with HTTP 400 |
| read a customer's accounts | succeeds | 8 of 8 succeed |
| transfer between accounts | succeeds | 8 of 8 succeed |

Reads and updates are fine. Creation is not, and the container log says why:

```
java.sql.SQLIntegrityConstraintViolationException: integrity constraint violation:
unique constraint or index violation ; SYS_PK_10116 table: ACCOUNT
```

Two concurrent inserts compute the same primary key. Consecutive customer ids come out 111
apart, which is a generated-id scheme with no lock around it.

**The error message is a lie, and it is the same lie as before.** ParaBank answers a collided
registration with *"This username already exists."* — for a 48-bit random name that certainly
does not exist, and it writes the same sentence into its own log. ADR 0013 records this exact
message appearing for a different wrong reason, a username one character too long. Twice now,
this application has reported a failure as a uniqueness conflict when it was nothing of the
kind. **A failure message from the system under test is evidence, not a diagnosis.**

The failure distribution follows from that, web suite, twenty runs, fifty failed steps:

| share | failure |
|---|---|
| 50% | registration refused: "This username already exists" |
| 36% | a page never finished loading, because the customer has no accounts to show |
| 6% | the funding dropdown did not contain the scenario's own account |
| 4% | the account link never appeared in the overview |
| 2% | the error message never appeared |
| 2% | the overview showed a different account than the fixture had recorded |

Only the first row is a cause. The rest are what a scenario looks like after its fixture was
refused. The last row is the one genuinely strange case: the fixture's REST read and the
browser's read of the same customer's accounts disagreed, which is this application's
id-allocation race producing wrong data instead of an exception.

## What the measurement found in our own code

Parallelism was not the only thing being tested, and it caught something honest.

`LoginPage.logInAs` clicked the submit button and returned. `click()` does not wait for the
navigation it starts, so the step finished while the login POST was still in flight and the
next step's `driver.get()` raced it: the browser asked for `openaccount.htm` and ended up
reporting `overview.htm`, where the late login response was going.

It hid well. The scenario that transfers money navigates to `overview.htm` too, so the race
was invisible there — only the scenario that opens an account navigates somewhere else, and
only that scenario failed. Measured before and after a `stalenessOf` wait on the clicked
button:

| configuration | before | after |
|---|---|---|
| web, single-threaded, 12 runs | 11 passed | **12 passed** |
| web, feature level, 12 runs | 6 passed | **12 passed** |

This was a real defect in CP 3.1's page object, present single-threaded, found only because
this checkpoint ran the suite two dozen times instead of once. It is also the defect
[ADR 0017](0017-locator-strategy-for-an-unmodifiable-ui.md) predicted: a page interaction that
does not wait for its own result fails under timing pressure and nowhere else. The twenty
scenario-level runs quoted above were re-measured after the fix; they moved from 0/20 to 1/20,
which is how we know the application, not the race, is the wall.

## Decision

**Scenario level is the right level, and it is off for this target.**

The framework supports parallel execution and does not choose it. The wiring is per suite and
lives in the consumer's build, not in any framework module: `trident-demo-parabank` sets it
from Maven properties because its three suites share one `junit-platform.properties` and
disagree, and the archetype generates a documented, switched-off block in the generated
project's own `junit-platform.properties`.

`trident-demo-parabank` ships with `trident.parallel.enabled=false`, and the configuration
below it stays wired. One word turns it on for a target that can take it:

```bash
./mvnw -Pweb verify -Dtrident.parallel.enabled=true
```

The smoke suite is pinned single-threaded explicitly rather than by omission. One scenario
gains nothing, and the quickstart's promise is that the fast run is the simple one.

**The execution gates were re-verified under interleaving,** because a gate that counts
correctly in a serial stream may not when messages from four scenarios are interleaved. Ten
parallel runs with partial passes, comparing the gate's count against Cucumber's own summary:
agreement on all ten, at every partial count from one to six. The gate keys everything by
`testCaseStartedId` rather than by position, so this is structural as well as measured.

## Consequences

- The isolation claim in ADR 0013 survived its first real test. Nothing in this investigation
  had to change about fixtures, the scenario context, or the driver.
- The reference suite stays as fast as it was, which is to say the baseline stands: the web
  suite is ~19s wall and the api suite ~14s.
- Anyone who turns this on against ParaBank will get a red build, and the comment at the
  property tells them why before they spend an afternoon on it.
- We now know the reference target's ceiling. Any future work that needs concurrency here needs
  a container per thread, not a thread-count setting.
- The generated project carries the warning rather than the setting, which is the honest
  division: we know what we measured about our target and nothing about theirs.

## Alternatives rejected

**Feature-level parallelism, the stated fallback.** Measured rather than assumed, and it is not
a fallback here. The web suite has one feature file, so feature level is single-threaded with
extra configuration — verified: one pool thread, twelve runs, identical to running serially.
The api suite has two feature files, so it gets two-way concurrency, which the curl table above
already prices at a 50% registration failure rate; measured, it passed 8 of 12. A fallback that
is either a no-op or still broken is not a fallback.

**Retries.** Rejected on principle and on this evidence. A retry here would convert a 50%
application defect into a slower green build and would have hidden every row of the diagnosis
above — including our own `LoginPage` race, which is the one thing in this investigation that
was actually our fault. `runbook-failing-test.md` says a flake is usually the suite's fault;
this time it was mostly not, and the only way to know that was to refuse the retry and read the
container's log. A retry would have been a decision to never find out.

**Lower the thread count.** The obvious first move, and the measurements say it does nothing:
concurrency 2 already fails half of all registrations. There is no safe number above one.

**Serialise the operations ParaBank cannot do concurrently,** with a lock around registration
and account creation while the rest of each scenario runs in parallel. Tempting, because
registration is fixture setup and costs ~0.1s. Rejected because one of the four web scenarios
*opens an account through the UI* as its subject, so the lock would have to cover the thing
under test, and a suite that serialises what it is testing is measuring something else. It also
buys very little: with ~5s of warm-up in a ~19s run, the ceiling on this suite is not worth a
lock nobody can explain later.

**One container per thread.** The real answer for a target that cannot take concurrent use, and
the shape is already in the codebase — it is what `DriverProvider` does for browsers. Rejected
for now on cost and scope: four ParaBank containers is roughly 2GB and ~8s of startup, the base
URI would have to become thread-scoped rather than a single system property resolved once per
JVM, and none of that is parallel execution — it is a second isolation mechanism. Recorded here
so the next person does not rediscover it: **when the application under test cannot be made
concurrent, the unit of isolation stops being the scenario and becomes the instance.**
