# 3. Picocontainer for step-definition scope

## Status

Accepted.

## Context

Step definitions in one scenario need to share state: a step stores a value and a later step
reads it. Cucumber instantiates each glue class itself, so the classes cannot simply hand
each other references, and the sharing mechanism has to guarantee that one scenario never
sees another's data.

Trident will run scenarios in parallel from Phase 3, and will later hold browser and driver
instances in the same place. Whatever carries scenario state has to survive that.

## Decision

`cucumber-picocontainer`. Glue classes declare what they need as constructor parameters:

```java
public class ConfigurationSteps {
    private final ScenarioContext context;

    public ConfigurationSteps(ScenarioContext context) {
        this.context = context;
    }
}
```

Picocontainer creates one `ScenarioContext` per scenario and gives that same instance to
every glue class in the scenario, then discards it.

`ScenarioContext` is a plain object with a public no-arg constructor, a private `HashMap`, and
no static fields or `ThreadLocal`. Glue classes hold no static state either — `Hooks` uses an
instance logger rather than the customary `private static final Logger` for that reason.

## Consequences

- Scenario isolation is structural. There is no shared static map to clear, so a leak between
  scenarios would require deliberately introducing static state.
- Parallel execution needs no extra work: separate scenarios get separate object graphs, so
  Phase 3 does not have to revisit this.
- A glue class's dependencies are visible in its constructor signature.
- Picocontainer needs no configuration file and no annotations, so there is nothing to keep in
  sync, at the cost of the wiring being invisible — the `cucumber-picocontainer` dependency is
  the only evidence it happens.
- Objects must have a constructor Picocontainer can call. This is a real constraint on future
  driver and client classes.

## Alternatives rejected

**A static holder or singleton `ScenarioContext`.** No dependency and no wiring. Rejected
outright: one scenario's data would be visible to the next, and it would break the moment
Phase 3 enables parallel execution.

**`ThreadLocal<ScenarioContext>`.** The usual fix for the static holder, and it does isolate
parallel scenarios. Rejected because it is still global state with a lifecycle nobody owns —
it must be cleared by hand, a missed clear leaks silently, and it breaks as soon as a scenario
touches more than one thread, which browser and mobile automation do.

**`cucumber-spring`.** More capable, and familiar to Spring teams. Rejected as far too much
machinery for passing a map between steps; it would make an application context part of
Trident's public contract for consumers who may not use Spring at all.

**`cucumber-guice`.** Comparable to Picocontainer and equally workable. Rejected only because
it requires explicit module configuration for no benefit at this scale; Picocontainer needs
none.
