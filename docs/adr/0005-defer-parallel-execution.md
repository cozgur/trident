# 5. Defer parallel execution

## Status

Accepted.

## Context

`cucumber-junit-platform-engine` can run scenarios in parallel by setting
`cucumber.execution.parallel.enabled=true`. It is one line, and it is tempting to set it while
the runner is being wired rather than coming back later.

Phase 0 has one scenario. The whole suite finishes in well under a second.

## Decision

`cucumber.execution.parallel.enabled=false`, stated explicitly in
`junit-platform.properties` with a comment pointing at this decision. Surefire's `forkCount`
stays at its default of 1, and neither `parallel` nor `threadCount` is configured.

Parallel execution arrives in Phase 3, alongside Selenium, when there is a suite whose runtime
justifies it and real shared resources to reason about.

## Consequences

- Runs are deterministic until then. A failure is the code's fault, not the
  scheduler's.
- The design does not foreclose the change: scenario state is per-scenario through
  Picocontainer with no static or `ThreadLocal` state, so turning the flag on in Phase 3 is a
  configuration change rather than a refactor. See
  [ADR 0003](0003-picocontainer-for-step-scope.md).
- Suite runtime will grow until Phase 3. This is acceptable while it is measured in seconds.
- The flag is written out as `false` rather than omitted, so the default is a decision on the
  record instead of an accident.

## Alternatives rejected

**Enable parallel execution now, while the runner is being wired.** Appealing because the
configuration is fresh in mind and it avoids revisiting the file. Rejected because it buys no
wall-clock time on a one-scenario suite while introducing a class of intermittent failures,
and because the first genuinely parallel-unsafe thing in the codebase will arrive with
Selenium in Phase 3 — enabling it earlier means the flag has never been tested against
anything that could break under it.

**Leave the property unset and rely on the default.** The default is already `false`.
Rejected because an absent property does not record whether anyone considered it; a future
contributor cannot tell a deliberate choice from an oversight.

**Parallelise through Surefire `forkCount` instead.** Rejected because it parallelises at the
JVM level, not the scenario level: each fork would re-run the whole suite discovery, and
Cucumber's own scheduler is the thing Phase 3 actually needs.
