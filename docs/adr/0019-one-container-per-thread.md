# 19. One container per thread: it works, and it is slower

## Status

Accepted as a record. **The mechanism is not shipped** — it was built, measured and then
deleted. What is kept here is the design and the numbers, which is the part worth keeping.

See [NON-GOALS](../NON-GOALS.md).

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

**The mechanism is deleted. This record is what survives it.**

It shipped switched off for about a day, on the reasoning that ADR 0018 had kept the parallel
wiring it disabled, so this could be kept too. That reasoning does not hold, and the difference
is worth stating because it is the general rule:

- What ADR 0018 kept switched off is **one configuration property with its reason beside it**.
  Nothing reads it unless you set it, there is no second path through the code, and a future
  change cannot break it without noticing.
- What this was is **about sixty lines and a second lifecycle path with no caller**. Every
  future change to the container lifecycle would have had to be read against it, tested against
  it, and would eventually have broken it — earning nothing, because nothing ran it.

Code with no caller is not an option, it is a liability with documentation attached. The
approach and the measurements are what a future phase needs, and they are here. Rebuilding from
this record is a morning's work, and it starts from a known answer instead of a blank page.

The two facts that would otherwise have to be rediscovered:

**No framework change is needed.** `RequestSpecFactory.from(config)` returns a specification
carrying the configured base URI, and `spec.baseUri(...)` replaces it. The override belongs in
the project that knows its target comes in instances, applied per request, and `ConfigProvider`
never learns that instances exist.

**The shape that worked.** Start N instances concurrently in `@BeforeAll`; hold their addresses
in a queue; let each thread claim one on first use and keep it in a `ThreadLocal`; warn loudly
when there are more threads than instances, because sharing silently returns you to the defect
in ADR 0018.

## Consequences

- The api suite is as fast as it was, and `ParaBankLifecycle` has one lifecycle path: start one
  container, stop one container.
- Anyone who needs concurrency against an application that cannot take it has the approach, the
  seam it hangs on, and a number to beat, without a dormant implementation to maintain.
- This record describes code that no longer exists, which is a real cost: prose can drift from
  a codebase in a way that a compiled path cannot. It is the smaller cost of the two.
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

**Keep it as an opt-in flag.** What this record originally decided, and it was wrong. See the
Decision above: the cost of an unused code path is paid by every future reader of the lifecycle,
and it is paid whether or not anyone ever sets the flag.

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
