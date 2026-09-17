# 8. Framework modules know no target application

## Status

Accepted.

## Context

Trident's claim is that adopting it means generating a project and writing feature files, not
forking a template and maintaining a copy of the plumbing. Everything else in the design serves
that claim.

`trident-runner` contradicted it. The module held the generic suite wiring — the Cucumber
lifecycle hooks, the JUnit Platform suite configuration, the tag routing — in the same place as
ParaBank's feature files, step definitions and fixtures, all in test scope. It could not be
published: a consumer depending on it would receive a demo bank's registration form, and there
was nothing to depend on anyway, because the useful classes were test sources.

The claim was therefore unproven in the one repository that exists to prove it.

## Decision

Framework modules — `trident-core`, `trident-api`, `trident-runner` — contain no knowledge of
any application under test. Nothing about ParaBank, Testcontainers or Docker appears in their
sources or their POMs.

`trident-runner` became publishable. Its suite wiring moved from test scope to main scope, and
`TridentSuite` is an abstract base carrying `@Suite` and `@IncludeEngines("cucumber")`. A
consumer subclasses it and declares only which features it wants:

```java
@SelectClasspathResource("features")
public class CheckoutTestSuite extends TridentSuite {}
```

That works because the JUnit Platform's suite annotations are `@Inherited`, which is a property
of a third-party library and is therefore asserted by a test rather than assumed.

Everything target-specific — the container, the fixtures, the glue, the feature files, the
Surefire and Failsafe configuration a consumer copies — lives in `trident-demo-parabank`. It
sets `maven.deploy.skip`, and `trident-bom` deliberately does not list it: the BOM manages
publishable artifacts, and listing the demo would invite someone to depend on ParaBank
fixtures.

## Consequences

- The reference target is replaceable. Phase 2 adds a second one against a different
  application, and the framework should not need a line changed.
- **The rule is enforced, not asserted.** CI greps the three framework modules' sources and
  POMs for `parabank`, `testcontainers` and `docker`, and fails the build on any match. This
  matters more than it sounds: the first implementation of this split leaked immediately, and
  not through code — `TridentSuite`'s javadoc pointed at `trident-demo-parabank/pom.xml` for an
  example. A reviewer would likely have passed it. The grep did not.
- The glue packages had to be split as well, and that is a separate decision with its own
  measurements: Cucumber's `@BeforeAll` runs for every glue package a suite loads, so a
  container lifecycle sitting on a shared glue path started ParaBank for the Docker-free smoke
  run while every test still passed. See
  [ADR 0010](0010-two-suites-surefire-and-failsafe.md).
- A consumer project must write two files the demo already shows: a suite subclass and the
  plugin configuration. Phase 2's archetype generates both, which is the point of doing the
  archetype before the web and mobile layers.
- Development is slightly slower. A change to the hooks means editing a published module and
  re-running the demo to see it work, where before both were one module.

## Alternatives rejected

**One runner module, with tags separating framework concerns from target concerns.** Simplest,
and the structure the project already had. Rejected because tags cannot keep files out of a
published jar: the artifact would ship ParaBank's registration fixture and feature files to
every consumer, and the claim that the framework is target-agnostic would rest on nobody
looking inside.

**The demo in a separate repository.** Cleanest separation of all, and the obvious end state
for a showcase. Rejected for now because the reference implementation has to build and run in
the same CI that enforces the leak check — a demo in another repository proves the framework
works over there, not that this repository's published modules are clean. Phase 2 adds
`trident-showcase` as a separate repository consuming from Maven Central, which answers a
different question: whether an outside project can adopt Trident at all.

**Publishing the demo module as an example artifact.** Tempting, since people do learn from
examples. Rejected because a published artifact is something people depend on, and a demo that
someone depends on stops being free to change.
