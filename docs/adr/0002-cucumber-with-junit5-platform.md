# 2. Cucumber on the JUnit 5 Platform

## Status

Accepted.

## Context

Trident's scenarios are written in Gherkin, and they have to be executed by something that
Maven, IDEs and CI already understand. Cucumber-JVM offers several runners, and the choice
determines how scenarios are discovered, filtered, reported and eventually parallelised.

Phase 3 will add parallel execution and Allure reporting, so the runner chosen now has to be
the one that supports those without being replaced.

## Decision

Cucumber runs through `cucumber-junit-platform-engine`, selected by a JUnit Platform suite:

```java
// Framework: the abstract base, published in trident-runner.
@Suite
@IncludeEngines("cucumber")
public abstract class TridentSuite {}

// Consumer project: declares only which features it wants.
@SelectClasspathResource("features")
public class ParaBankTestSuite extends TridentSuite {}
```

Engine configuration — glue path, plugins, parallelism — lives in
`src/test/resources/junit-platform.properties`. The tag filter is the exception: Surefire
passes `cucumber.filter.tags` into the forked JVM as a system property, forwarded from the
active Maven profile's `${trident.tags}`.

Surefire in `trident-runner` includes `**/*TestSuite.java`, because the suite class matches
none of Surefire's default naming patterns.

## Consequences

- Scenarios are ordinary JUnit Platform tests. Maven, IntelliJ and CI discover and report them
  with no Cucumber-specific tooling.
- Tag filtering is a Maven profile concern, so `-Psmoke` and `-Pregression` select scenarios
  without editing any file.
- The engine supports parallel execution through the same properties file, so Phase 3 turns
  one flag on rather than changing runners.
- The suite class is a container: Surefire counts it as a test in its own right. This makes
  `failIfNoTests` useless as evidence that scenarios ran, and is why execution is asserted
  separately. See [ADR 0007](0007-pinning-surefire-and-verifying-execution.md).
- Configuration is split between `junit-platform.properties` and the Surefire block. Both
  places carry comments saying why, because the split is not obvious.

## Alternatives rejected

**`@RunWith(Cucumber.class)` (the JUnit 4 runner).** The most widely documented option.
Rejected because it pins the project to JUnit 4, needs `junit-vintage-engine` to run beside
JUnit 5 tests, and its parallelism story is worse — exactly the constraint Phase 3 runs into.

**`@CucumberOptions` on the runner class.** Keeps configuration next to the runner and is
familiar. Rejected because it belongs to the JUnit 4 runner, and because it puts the tag
filter in compiled code where a Maven profile cannot reach it without a rebuild.

**Cucumber's CLI (`io.cucumber.core.cli.Main`) via exec-maven-plugin.** Full control over
invocation. Rejected because it leaves the JUnit Platform entirely: no IDE discovery, no
Surefire reports, and reporting would have to be rebuilt from scratch.

**TestNG.** Rejected for having no advantage here; the project has no TestNG tests and JUnit 5
is where the Cucumber engine is best supported.
