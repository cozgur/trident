# Trident

Trident is a Java test automation framework that covers web, mobile and API testing behind a
single Cucumber runner. It is published as a Maven artifact, so a project adds it as a
dependency and writes feature files and step definitions of its own rather than forking a
template repository and maintaining a copy of the plumbing. This repository is the framework
itself; Phase 0 contains the build skeleton, configuration resolution, the scenario context
and one scenario that proves the runner is wired end to end.

## Run your first test in 5 minutes

Git and Java 21 are the only prerequisites. Maven comes from the wrapper committed in this
repository, so there is nothing else to install.

```bash
git clone https://github.com/cozgur/trident.git
cd trident
./mvnw -Psmoke verify
```

The run ends with:

```
1 Scenarios (1 passed)
4 Steps (4 passed)
```

That scenario loads the framework's configuration, checks that values fall back correctly
between the two shipped property files, and passes a value from one step to the next through
the scenario context. If it passes, your toolchain is set up correctly.

## Modules

| Module | Contents |
|---|---|
| `trident-bom` | Bill of materials. Consumer projects import this to depend on Trident modules without declaring versions. |
| `trident-core` | Configuration resolution and the per-scenario context. |
| `trident-api` | API testing support. Placeholder until Phase 1. |
| `trident-web` | Web testing support. Placeholder until Phase 3. |
| `trident-mobile` | Mobile testing support. Placeholder until Phase 3. |
| `trident-runner` | Cucumber and JUnit Platform wiring, and the smoke suite. |
| `trident-archetype` | Project archetype. Placeholder until Phase 4. |

## Configuration

Values resolve in two stages. First the active environment: the `env` system property, then
the `ENV` environment variable, then `local`. Then the values themselves, highest precedence
first:

1. system properties
2. environment variables
3. `classpath:config/<env>.properties`
4. `classpath:config/default.properties`

### Overriding a single value

Use a system property or the mapped environment variable. Both sit above every file layer, so
they override one key and leave the rest of the shipped defaults alone:

```bash
./mvnw -Psmoke verify -Dweb.base.url=https://staging.example.com
WEB_BASE_URL=https://staging.example.com ./mvnw -Psmoke verify
```

The five keys and their variables are `env`/`ENV`, `web.base.url`/`WEB_BASE_URL`,
`api.base.url`/`API_BASE_URL`, `timeout.default.seconds`/`TIMEOUT_DEFAULT_SECONDS` and
`timeout.polling.millis`/`TIMEOUT_POLLING_MILLIS`.

### Overriding with a file

Add `config/<env>.properties` to your own resources and select it with `-Denv=<env>`. This is
a different source path from `config/default.properties`, so the two merge: keys you define
win, and keys you leave out fall back to Trident's shipped defaults.

Supplying your own `config/default.properties` works differently, and the difference matters.
Your copy is earlier on the classpath, so it **shadows** Trident's file rather than merging
with it — `MERGE` combines the two distinct source paths, not two copies of the same path. A
partial file therefore leaves the keys you omitted unresolved, and reading one throws a
`NullPointerException` rather than returning a default. If you replace
`config/default.properties`, supply a complete file defining all five keys.

## Formatting

Spotless formats with palantir-java-format and runs as part of `verify`. To apply it:

```bash
./mvnw com.diffplug.spotless:spotless-maven-plugin:apply
```

The fully-qualified form is used because `trident-bom` does not inherit from the parent, so
the short `spotless:apply` prefix does not resolve across the whole reactor.

## Running a subset

The `smoke` profile is active by default and runs scenarios tagged `@smoke`. The `regression`
profile runs everything not tagged `@wip`.

```bash
./mvnw -Psmoke verify
./mvnw -Pregression verify
```

## Roadmap

| Phase | Scope |
|---|---|
| 0 | Build skeleton and runner wiring — sealed at v0.1.0 |
| 1 | `trident-api` + `trident-demo-parabank`, ParaBank via Testcontainers, failsafe bound |
| 2 | `trident-archetype`, Maven Central publish, `trident-showcase` against a second target |
| 3 | `trident-web` (Selenium 4), parallel execution, Allure |
| 4 | `trident-mobile` (Appium 3 server / java-client 10) |
| 5 | Documentation package and portfolio case study |

The archetype comes before web and mobile on purpose. It is the proof of the central claim —
that a new application needs only a generated project and feature files — and proving it after
two more layers would mean unpicking the target-specific assumptions those layers accumulate.

## Reference target: ParaBank

Trident is developed against [ParaBank](https://github.com/parasoft/parabank), a banking demo
application from around 2005. It was chosen because it is the hard case: a DOM with no test
hooks, SOAP and REST that disagree, and global state. A framework proven against it does not
depend on the target being friendly.

ParaBank lives entirely in `trident-demo-parabank`. No framework module references it, and CI
proves that with a grep over `trident-core`, `trident-api` and `trident-runner`. See
[working with a legacy application](docs/working-with-legacy.md) for what that choice implies.

## Documentation

| Document | Who it is for |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | A developer on the team who needs to run the suite, add a scenario, or find out what it will not ask of them. |
| [Test strategy](docs/test-strategy.md) | Anyone deciding whether the coverage is enough, including people who do not write code. |
| [Runbook: a test failed](docs/runbook-failing-test.md) | Whoever is looking at red CI right now. |
| [Working with a legacy application](docs/working-with-legacy.md) | Anyone asking why the target is a 2005 JSP app and what that changes. |

## Design decisions

Decisions are recorded as ADRs in [`docs/adr/`](docs/adr/). Things Trident deliberately does
not do are listed in [`docs/NON-GOALS.md`](docs/NON-GOALS.md).

## License

MIT. See [LICENSE](LICENSE).
