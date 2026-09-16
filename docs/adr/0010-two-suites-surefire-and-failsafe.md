# 10. Two suites: Surefire for smoke, Failsafe for API

## Status

Accepted.

## Context

Phase 1 adds scenarios that exercise a real ParaBank instance in a Docker container. Phase 0
shipped one scenario that needs nothing but a JVM, and a README quickstart promising that Git
and Java 21 are the only prerequisites.

Those two kinds of test have different costs. The wiring scenario finishes in under a second
and is the thing a newcomer runs first. A container-backed scenario needs Docker, an image
pull, and a JSP application's cold start before it can assert anything.

Putting both behind one command would make Docker a prerequisite for the quickstart.

## Decision

Two suites, one per Maven test plugin.

`TridentTestSuite` runs under Surefire and executes `@smoke` scenarios. No Docker.
`TridentIT` runs under Failsafe — matched by `**/*IT.java` — and executes `@api` scenarios.
Docker required.

Both select the same feature tree with `@SelectClasspathResource("features")`. They are kept
apart by tag, not by directory.

Maven profiles decide which suite runs:

| profile | tags | suite | Docker |
|---|---|---|---|
| `smoke` | `@smoke` | Surefire only | no (default; the quickstart) |
| `api` | `@api` | Failsafe only | yes |
| `regression` | `not @wip` | Surefire only | no |

`regression` is Surefire-only on purpose. It exists so a developer can run the full local
suite with one command; requiring Docker would make it a second `api` profile. CI runs
`smoke` and `api` as separate steps, with Docker a precondition of the `api` step alone.

### Each suite composes its own tag expression

This is the part CP 1.1 surfaced, and it is the reason this ADR is not simply "two plugins".

The obvious wiring is to forward `${trident.tags}` verbatim into each forked JVM, as Phase 0
did for Surefire. That is wrong as soon as a second tag exists. Under `regression` the
expression is `not @wip`, which matches `@api` scenarios perfectly well — so Surefire, the
Docker-free suite, would have discovered and executed container-backed scenarios and failed
with connection errors.

Each plugin therefore composes the expression rather than passing it through:

```
Surefire:  @smoke and (${trident.tags})
Failsafe:  @api   and (${trident.tags})
```

Routing is now structural. A suite cannot execute the other's scenarios for any value of
`${trident.tags}`, including one supplied on the command line. This was checked
adversarially rather than assumed:

| forced value | result |
|---|---|
| Surefire with `-Dtrident.tags=@api` | 0 scenarios |
| Failsafe with `-Dtrident.tags=@smoke` | 0 scenarios |
| Surefire with `-Dtrident.tags='not @wip'` | 1 scenario — the `@smoke` one only |

## Consequences

- The quickstart stays Docker-free, and CI proves it: the smoke steps run and their gate
  passes before Docker is so much as checked for.
- Two message logs. `cucumber.plugin` moved out of `junit-platform.properties` and into each
  plugin block, because a shared log would have Failsafe overwrite Surefire's and the
  execution gate would assert against the wrong run. See
  [ADR 0007](0007-pinning-surefire-and-verifying-execution.md).
- The execution gate takes `--reports` and `--messages` and runs once per suite. It is
  deliberately not forked into two scripts: the negative control only means anything while
  both suites are measured by identical code.
- A new tag needs a decision about which suite owns it, and a scenario carrying neither
  `@smoke` nor `@api` runs nowhere. That is the cost of structural routing, and it is
  preferable to a scenario silently running in the wrong suite.
- Adding a tag to the composition is a POM change, not a feature-file change. The coupling is
  explicit and lives in one place.

## Alternatives rejected

**A single Failsafe suite for everything.** Simpler: one runner, one plugin, one report
directory, no tag composition. Rejected because it makes Docker a prerequisite for running
any test at all, which breaks the quickstart promise that Git and Java 21 are enough. That
promise is the framework's first impression and worth more than the simplification.

**Forwarding `${trident.tags}` verbatim to both suites.** The direct extension of Phase 0's
wiring. Rejected because `not @wip` then routes `@api` scenarios into the Docker-free suite,
as described above. The failure would appear as connection errors in `regression` only —
green in `smoke` and green in `api` — which is exactly the kind of bug that survives a review.

**Separating the suites by directory** (`features/smoke` and `features/api`, each suite
selecting its own path). Also structural, and arguably simpler to read. Rejected because it
ties a scenario's runner to its location on disk: moving a feature file would silently change
which suite runs it, and a scenario belonging to both would have to be duplicated. Tags
describe what a scenario needs; directories describe how someone chose to file it.

**Running the API suite under Surefire with a Docker check that skips when absent.** Keeps
one plugin. Rejected because a suite that silently skips is worse than one that is not run:
CI would go green on a runner without Docker, having tested nothing, which is the failure
mode [ADR 0007](0007-pinning-surefire-and-verifying-execution.md) exists to prevent.
