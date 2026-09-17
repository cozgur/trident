# Architecture decision records

Each record states what was decided, why, and what was given up. Most of them exist because
something was measured and the measurement contradicted the obvious choice; where that
happened, the numbers are in the record.

| # | Decision | Phase |
|---|---|---|
| [0001](0001-maven-over-gradle.md) | Maven over Gradle | 0 |
| [0002](0002-cucumber-with-junit5-platform.md) | Cucumber on the JUnit 5 Platform | 0 |
| [0003](0003-picocontainer-for-step-scope.md) | Picocontainer for step-definition scope | 0 |
| [0004](0004-two-stage-config-resolution.md) | Two-stage configuration resolution | 0 |
| [0005](0005-defer-parallel-execution.md) | Defer parallel execution | 0 |
| [0006](0006-standalone-bom.md) | `trident-bom` is a standalone POM | 0 |
| [0007](0007-pinning-surefire-and-verifying-execution.md) | Pin Surefire and verify execution independently of the build's exit code | 0 |
| [0008](0008-framework-modules-know-no-target.md) | Framework modules know no target application | 1 |
| — | *0009 was planned and never written* | — |
| [0010](0010-two-suites-surefire-and-failsafe.md) | Two suites: Surefire for smoke, Failsafe for API | 1 |
| [0011](0011-readiness-probe-triggers-schema-init.md) | The readiness probe targets the home page, not the layer under test | 1 |
| [0012](0012-rest-assured-bypassed-for-non-json-error-bodies.md) | The rejection scenario bypasses REST Assured | 1 |
| [0013](0013-scenario-owned-fixtures.md) | Scenarios own their fixtures | 1 |

## About 0009

Phase 1 planned an ADR numbered 0009 on fixture ownership. It was never written, and the
subject is covered by [0013](0013-scenario-owned-fixtures.md), which was written later with the
measurements Phase 1 had produced by then.

The number is left unused rather than reassigned. An ADR number is a stable reference — commit
messages and other records cite them — so reusing 0009 for something else would make an old
citation point at the wrong decision. A gap costs a line of explanation; a silently reused
number costs somebody an afternoon.

## Writing a new one

Copy the shape of any existing record: **Status**, **Context**, **Decision**, **Consequences**,
**Alternatives rejected**. The last section is not optional. A decision with no rejected
alternative is not a decision, and the reason an option lost is the part a future reader needs
when they are about to choose it again.

Number the next one 0014.
