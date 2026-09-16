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

**No parallel execution before there is a suite large enough to need it.** Parallelism buys
wall-clock time at the cost of a class of intermittent failures, and paying that cost for a
suite that finishes in seconds is a bad trade. See
[ADR 0005](adr/0005-defer-parallel-execution-to-phase-2.md).

**No convenience methods on `ScenarioContext` until a scenario needs them.** `size()`,
`remove()` and `keySet()` are easy to add and hard to remove once step definitions depend on
them, so they wait for a concrete use.
