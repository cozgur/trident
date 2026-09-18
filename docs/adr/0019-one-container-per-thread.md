# 19. One container per thread: it works, and it is slower

## Status

Accepted, as an option that is off by default.

## Context

[ADR 0018](0018-parallel-execution-at-scenario-level.md) ended with a wall. ParaBank cannot
create two rows at once — concurrent inserts collide on a generated primary key — so scenario
level parallelism against a single instance passed 0 of 20 runs. That record named the forward
path without taking it: **when the application under test cannot be made concurrent, the unit
of isolation stops being the scenario and becomes the instance.**

This is that trial, deliberately bounded: the api suite, no browser, four instances.

## The design question, answered before anything was built

The api base URI is a system property that `ConfigProvider` resolves once and caches for the
life of the JVM. Four instances need four addresses, so the question was whether a thread-scoped
base URI forces a change to `trident-core`.

It does not, and that was established with a test rather than an argument.
`RequestSpecFactory.from(config)` returns a `RequestSpecification` carrying the configured base
URI, and a caller may replace it:

| specification | base URI |
|---|---|
| as the framework builds it | `http://localhost` |
| after the caller overrides it | `http://per-thread.example:9999` |

So the override happens in the project that knows its target comes in instances, on the
specification, at the moment of the request. No setter on `ConfigProvider`, no thread-scoped
configuration, and no framework module told that instances exist. Configuration stays what it
has been since ADR 0004: an input to a run, not something a test may change underneath other
tests.

## What was measured

Everything below is the api suite, eight scenarios, on a 10-core laptop with 8 GiB allotted to
Docker.

| configuration | passed | container startup | scenario phase | wall clock |
|---|---|---|---|---|
| 1 instance, 1 thread (shipped) | 20 of 20 | 7.7s | 3.4s | 14–15s |
| 1 instance, 4 threads (ADR 0018) | **0 of 20** | 7.7s | — | 14–16s |
| **4 instances, 4 threads** | **20 of 20** | **14.2s** | **2.8s** | **19–23s** |

Peak memory, sampled every two seconds: about 430 MiB per instance, so roughly 1.7 GiB for
four against the 8 GiB available.

**The correctness result is unambiguous.** Giving each thread its own application turns 0 of 20
into 20 of 20. The isolation ADR 0013 built at the scenario level composes with isolation at the
instance level, and nothing about fixtures, the scenario context or the suites had to change.

**The performance result is just as unambiguous, in the other direction.** Parallelism saved
0.6 seconds of scenario phase — 3.4s down to 2.8s — and the extra three containers cost 6.5
seconds of startup. Net: about 6 seconds worse, every run.

Worth noticing in the table above: the *total* scenario work rose from 3.3s to 10.6s. Each
scenario got roughly three times slower while four ran at once, because four JVM threads and
four application instances contend on one machine. Four-way concurrency bought a 1.2× speedup,
not a 4× one, and then paid twice the startup for it.

## Decision

The mechanism ships, off by default.

`trident.parabank.instances` is 1 unless set, which is byte-for-byte the behaviour that was
there before: one container, one address, the same value configuration already carried. Four
instances is one flag:

```bash
./mvnw -Papi verify -Dtrident.parallel.enabled=true \
  -Dtrident.parallel.strategy=fixed -Dtrident.parallel.threads=4 \
  -Dtrident.parabank.instances=4
```

It is kept rather than deleted for the same reason ADR 0018 kept the parallel wiring it had
just switched off: the measurement is only useful if the next person can reproduce it in one
command, and a target that is slower here may not be slower there. It is not a default, and
this record is the reason it is not.

## Consequences

- The api suite is as fast as it was, because nothing changed for it.
- Anyone who needs concurrency against an application that cannot take it now has a worked
  mechanism and a number to beat.
- `ParaBankLifecycle` carries a second mode. That is real complexity for a path nothing runs by
  default, and it is the main argument for deleting all of this; the counter-argument is that
  the alternative is a paragraph in a document describing code that does not exist.
- The break-even is calculable and it is far away. The fleet has to save 6.5 seconds in the
  scenario phase to pay for itself, and it saved 0.6. On these ratios the serial scenario phase
  would have to be roughly ten times longer — a suite of eighty-odd api scenarios rather than
  eight — before four instances came out ahead. That is a projection from two measurements, not
  a measurement.

## Alternatives rejected

**Make it the default for the api suite.** It is correct, after all, and correctness usually
wins over six seconds. Rejected because the single-instance suite is *also* correct — it passes
20 of 20 — so the six seconds buys nothing at this size. A default that is slower and no more
reliable is just a slower default.

**A thread-scoped base URI inside `trident-core`.** The obvious place to solve "each thread
needs a different address", and it would have made the demo simpler. Rejected because it was
not necessary — see the table above — and because it would have put a target's deployment shape
into the configuration system every consumer inherits. The framework supplies a base URI and
lets a caller override it; that is already the right seam.

**One container per scenario.** The cleanest isolation of all, and ADR 0011 already priced it:
this image takes about eight seconds to become ready, so eight scenarios would pay a minute of
startup to save three seconds of work. Instances-per-thread is the compromise that keeps the
startup cost proportional to the thread count rather than to the scenario count, and even that
did not pay.

**Reuse containers between runs** (`withReuse(true)`), which would amortise the startup this
trial is dominated by. Rejected on the grounds ADR 0013 already gives: a reused container
carries the previous run's customers and balances, and a suite whose scenarios own their data
must not inherit anyone else's. Trading isolation for six seconds is the wrong direction for a
project whose main claim is the isolation.
