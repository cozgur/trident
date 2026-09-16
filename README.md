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
| `trident-web` | Web testing support. Placeholder until Phase 2. |
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

A consumer project overrides a value by putting its own `config/default.properties` or
`config/<env>.properties` earlier on the classpath — its own `src/test/resources` is enough —
or per value at run time with `-Dweb.base.url=...` or `WEB_BASE_URL=...`.

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
| 0 | Build skeleton and runner wiring |
| 1 | `trident-core` + `trident-api`, ParaBank via Testcontainers, failsafe bound |
| 2 | `trident-web` (Selenium 4), parallel execution, Allure |
| 3 | `trident-mobile` (Appium 3 server / java-client 10) |
| 4 | `trident-archetype` + Maven Central publish + consumer showcase repo |
| 5 | Documentation package and portfolio case study |

## Design decisions

Decisions are recorded as ADRs in [`docs/adr/`](docs/adr/). Things Trident deliberately does
not do are listed in [`docs/NON-GOALS.md`](docs/NON-GOALS.md).

## License

MIT. See [LICENSE](LICENSE).
