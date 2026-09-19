# Non-goals

Things Trident deliberately does not do, and why. A non-goal is not a "not yet" — reopening
one needs a reason that has changed, not just a request.

**No AI self-healing locator layer.** A locator that repairs itself turns a real regression
into a silent pass, which is the opposite of what a test suite is for.

**No custom reporting UI.** Allure already renders Cucumber output well, and a bespoke
reporting front end would be a second product to maintain alongside the framework.

**No Gradle build.** One build system is enough for a project this size, and Maven's fixed
lifecycle keeps consumer-facing coordinates and the BOM straightforward. See
[ADR 0001](adr/0001-maven-over-gradle.md).

**No BDD for unit-level tests.** Gherkin earns its overhead when a scenario describes
behaviour a non-programmer would recognise; for a class's own edge cases it adds a parsing
layer between the test and the code, so those stay in JUnit.

**No parallel execution.** Deferred in Phase 0 because parallelism buys wall-clock time at the
cost of a class of intermittent failures, and paying that for a suite that finishes in seconds
is a bad trade — [ADR 0005](adr/0005-defer-parallel-execution.md). Phase 3 turned it on and
measured it: the suite was ready and the reference target was not. ParaBank cannot create two
rows at once, so scenario-level parallelism passed 1 of 20 runs against it. The wiring is one
property and it ships switched off. See
[ADR 0018](adr/0018-parallel-execution-at-scenario-level.md).

**One container per thread is not shipped.** It is the way to run a target that cannot take
concurrent use, and it works — 0 of 20 runs became 20 of 20. It is also about six seconds
slower per run, because four containers cost more startup than four threads save at this
suite's size. It was built, measured and then deleted: a second isolation mechanism with no
caller is read, tested and eventually broken by every future change to the lifecycle, and earns
nothing in between. The design and the numbers are kept in
[ADR 0019](adr/0019-one-container-per-thread.md), which is the part a future phase needs.

**No SOAP, although ParaBank exposes it.** The same data is reachable over REST in a shape
that is simpler to assert on, and supporting two protocols to test one application would double
the surface for no coverage.

**`cleanDB` and `initializeDB` are never called.** A global reset means one thing can run at a
time, forever — no parallel execution, no two engineers at once, no shared environment.
Scenarios own their data instead, and CI greps the source trees for both names.

**Testcontainers reuse is not enabled.** A reused container carries the previous run's
customers and balances; every clean run starts from a fresh application.

**No convenience methods on `ScenarioContext` until a scenario needs them.** `size()`,
`remove()` and `keySet()` are easy to add and hard to remove once step definitions depend on
them, so they wait for a concrete use.
