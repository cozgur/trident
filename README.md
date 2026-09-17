# Trident

Trident is a Java test automation framework that covers web, mobile and API testing behind a
single Cucumber runner. It is consumed as a Maven artifact, not forked: a project depends on
the modules it needs, imports [`trident-bom`](trident-bom/pom.xml) to avoid declaring versions,
and writes feature files and step definitions of its own. From Phase 2 the way to start is to
generate a project from `trident-archetype`, which produces the suite classes, the plugin
wiring and a first feature file; onboarding a new application should be a matter of writing
scenarios, not of rebuilding the plumbing.

The framework modules know nothing about any particular application. Trident is developed
against a reference target — see [ParaBank](#reference-target-parabank) below — which lives in
its own module and is never published.

## Run your first test in 5 minutes

Measured, not estimated: **45 seconds** from a clean clone with an empty `~/.m2`, and
**4 seconds** once the cache is warm. The first run is dominated by downloads — 430 artifacts,
76 MB — so on a slow connection expect minutes rather than seconds. That is the network, not a
broken build.

Adopting Trident in your own project is faster, because you generate rather than clone:
**9 seconds** from `archetype:generate` to a passing suite, measured with every
`dev.ozgurcetintas` artifact deleted from `~/.m2` so that all nine resolved from Central. See
[Start a project](#start-a-project).

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

## Start a project

Trident is on Maven Central. Generate a project rather than cloning this one:

```bash
mvn archetype:generate \
  -DarchetypeGroupId=dev.ozgurcetintas.trident \
  -DarchetypeArtifactId=trident-archetype \
  -DarchetypeVersion=0.2.1 \
  -DgroupId=com.example -DartifactId=my-suite -Dprefix=My
cd my-suite
mvn -Psmoke verify
```

That ends with a passing scenario and no editing. You get both suites, the tag routing, the
glue split and a first feature file already wired — the things this repository had to get wrong
once before getting right.

## Modules

| Module | Contents |
|---|---|
| `trident-bom` | Bill of materials. Consumer projects import this to depend on Trident modules without declaring versions. |
| `trident-core` | Configuration resolution and the per-scenario context. |
| `trident-api` | API testing support: request specifications built from configuration. |
| `trident-web` | Web testing support. Placeholder until Phase 3. |
| `trident-mobile` | Mobile testing support. Placeholder until Phase 4. |
| `trident-runner` | Suite base classes and Cucumber lifecycle hooks. Published. |
| `trident-archetype` | Project archetype. Placeholder until Phase 2. |
| `trident-demo-parabank` | Reference implementation against ParaBank. **Not published** — feature files, steps and fixtures live here. |

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

Three profiles, and only one of them needs Docker.

| Profile | Runs | Docker |
|---|---|---|
| `smoke` | `@smoke` scenarios under Surefire. Active by default. | no |
| `api` | `@api` scenarios under Failsafe, against a container. | **yes** |
| `regression` | everything not tagged `@wip`, on the Surefire side. | no |

```bash
./mvnw -Psmoke verify
./mvnw -Papi verify
./mvnw -Pregression verify
```

The quickstart above uses `smoke`, and it stays Docker-free on purpose. Only `-Papi` requires
Docker.

## Roadmap

| Phase | Scope |
|---|---|
| 0 | Build skeleton and runner wiring — sealed at v0.1.0 |
| 1 | `trident-api` + `trident-demo-parabank`, ParaBank via Testcontainers, failsafe bound — **done** |
| 2 | `trident-archetype`, Maven Central publish, and `trident-showcase` — a **separate repository**, not a module here, consuming Trident from Maven Central against a target unrelated to ParaBank |
| 3 | `trident-web` (Selenium 4), parallel execution, Allure |
| 4 | `trident-mobile` (Appium 3 server / java-client 10) |
| 5 | Documentation package and portfolio case study |

The archetype comes before web and mobile on purpose. It is the proof of the central claim —
that a new application needs only a generated project and feature files — and proving it after
two more layers would mean unpicking the target-specific assumptions those layers accumulate.

`trident-showcase` lives in its own repository for the same reason. A module inside this build
would prove nothing: it would share the reactor, the parent POM and the local repository, so it
could pass while the published artifacts were unusable to anyone outside. The showcase has to
resolve Trident the way a stranger would.

## Reference target: ParaBank

Trident is developed against [ParaBank](https://github.com/parasoft/parabank), a banking demo
application from around 2005. It was chosen because it is the hard case: a DOM with no test
hooks, SOAP and REST that disagree, and global state. A framework proven against it does not
depend on the target being friendly.

ParaBank lives entirely in `trident-demo-parabank`. No framework module references it, and CI
proves that with a grep over `trident-core`, `trident-api` and `trident-runner`. See
[working with a legacy application](docs/working-with-legacy.md) for what that choice implies.

It runs in a container, so **`-Papi` needs Docker**. The quickstart does not: `-Psmoke` never
starts a container, and CI proves that too, by running the smoke suite with `DOCKER_HOST`
pointing at a socket that does not exist.

## Documentation

| Document | Who it is for |
|---|---|
| [CONTRIBUTING.md](CONTRIBUTING.md) | A developer on the team who needs to run the suite, add a scenario, or find out what it will not ask of them. |
| [Test strategy](docs/test-strategy.md) | Anyone deciding whether the coverage is enough, including people who do not write code. |
| [Runbook: a test failed](docs/runbook-failing-test.md) | Whoever is looking at red CI right now. |
| [Working with a legacy application](docs/working-with-legacy.md) | Anyone asking why the target is a 2005 JSP app and what that changes. |

## Design decisions

Decisions are recorded as ADRs, indexed in [`docs/adr/`](docs/adr/README.md). Things Trident deliberately does
not do are listed in [`docs/NON-GOALS.md`](docs/NON-GOALS.md).

## License

MIT. See [LICENSE](LICENSE).
