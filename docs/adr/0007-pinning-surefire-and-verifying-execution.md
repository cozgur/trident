# 7. Pin Surefire and verify execution independently of the build's exit code

## Status

Accepted.

## Context

Two separate discoveries during Phase 0 showed that a green build is not evidence that any
scenario ran.

**Surefire 3.5.3 miscounts a JUnit Platform suite.** With that version pinned, a run printed
Cucumber's own `1 Scenarios (1 passed)` while the Surefire report recorded `tests="0"` with
no `<testcase>` element at all. The suite had demonstrably executed. Bisecting across
versions, 3.2.5, 3.5.0, 3.5.2, 3.5.4, 3.5.5 and 3.5.6 all report `tests="1"`; only 3.5.3
reports zero. It was caught only because `failIfNoTests` turned the miscount into a build
failure — had that flag been off, the build would have been green with no record of any test.

**`failIfNoTests` does not mean tests ran.** Running the suite with a tag that matches no
scenario shows the opposite failure:

```
./mvnw -pl trident-runner -am test -Psmoke -Dtrident.tags='@nonexistent'
→ 0 Scenarios
→ Tests run: 1, Skipped: 1     BUILD SUCCESS
```

Cucumber executed nothing, yet the build passed and the report claims one test, because
Surefire counts the JUnit Platform suite container itself. A gate that sums the `tests`
attribute would pass this run.

Both failures are silent in the same way: CI is green, and nothing was tested.

## Decision

Pin `maven-surefire-plugin` and `maven-failsafe-plugin` to **3.5.6**, with a comment in the
parent POM recording that 3.5.3 is skipped deliberately so nobody pins back to it.

Do not treat the build's exit code as evidence of execution. CI asserts it with two
independent checks, both of which must pass:

1. **Surefire**, computing `tests - skipped - failures - errors` summed across the runner's
   reports. Subtracting `skipped` is what distinguishes executed work from mere discovery.
2. **Cucumber's own message log.** `junit-platform.properties` adds the `message` plugin
   writing `target/cucumber-messages.ndjson`, and the gate counts scenarios whose every step
   finished `PASSED`. This is authoritative: a scenario whose steps are `UNDEFINED` or
   `PENDING` still surfaces as a non-skipped Surefire testcase, and only Cucumber's record
   tells the two apart.

Both numbers are printed before the assertion, so a CI failure is diagnosable from the log
without downloading artifacts.

The gate is followed by a **negative control**: a run filtered on `@nonexistent` must produce
zero passing scenarios. If it produces any, the tag filter is not reaching the forked test
JVM, every run is executing every scenario, and the gate is passing for the wrong reason.

## Consequences

- CI carries a step that tests the test infrastructure rather than the framework. It is
  labelled as such in the workflow, because its purpose is not obvious.
- The gate and the negative control share one script, so the two cannot drift apart and start
  measuring different things — which would defeat the control.
- A Cucumber message log is produced on every run. It is uploaded as an artifact on failure
  and is the record to read when the gate fails.
- Upgrading Surefire now requires checking that the suite still reports a non-zero executed
  count. The gate does this automatically, which is the point.
- `cucumber.filter.tags` must stay out of `junit-platform.properties`. It arrives as a system
  property forwarded by Surefire from `${trident.tags}`, and the negative control is what
  proves that forwarding works.

## Alternatives rejected

**Trust `failIfNoTests`.** It is built in and needs no scripting. Rejected because it checks
that a test *class* matched, not that anything ran: the `@nonexistent` run above satisfies it
while executing nothing.

**Assert on the `tests` attribute alone.** Simpler to parse and the obvious first idea.
Rejected for the same reason — Surefire counts the suite container, so `tests="1"` is
reported for a run with zero scenarios. Subtracting `skipped`, `failures` and `errors` is the
minimum that measures executed work.

**Assert only on Cucumber's message log and drop the Surefire check.** The message log is the
authoritative source, so the second check looks redundant. Rejected because the two fail
independently: the Surefire 3.5.3 bug showed the report can be wrong while Cucumber is right,
and a misconfigured message plugin would leave the log empty while Surefire is right. Keeping
both means a single broken layer cannot produce a false green.

**Pin Surefire to the latest version without recording why.** Rejected because 3.5.3 sits in
the middle of an otherwise healthy range; without the comment and this record, a future
version bump could land back on it and reintroduce a silent green.
