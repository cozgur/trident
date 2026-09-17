# ${artifactId}

A test suite built on [Trident](https://github.com/cozgur/trident).

## Run it

```bash
mvn -Psmoke verify
```

That is the suite you run before pushing. It needs Java 21 and Maven, nothing else, and it
should end with:

```
1 Scenarios (1 passed)
3 Steps (3 passed)
```

If that passes, your setup is fine and the example scenario can be replaced with your own.

## Where things go

| What | Where |
|---|---|
| Feature files | `src/test/resources/features/` |
| Step definitions | `src/test/java/${package}/steps/` |
| Everything else — clients, page objects, container lifecycles | `src/test/java/${package}/support/` |
| Configuration | `src/test/resources/config/default.properties` |

## Tags decide where a scenario runs

Every scenario needs exactly one of these, and a scenario with neither runs nowhere:

- `@smoke` — runs under Surefire in `${prefix}TestSuite`. Must stay fast and must not need a
  container. This is the promise that lets anyone run the suite.
- `@api` — runs under Failsafe in `${prefix}IT`, only under `-Papi`. Put slow or
  container-backed scenarios here.

The tag expression is composed per suite in `pom.xml` — `@smoke and (...)` for one, `@api and
(...)` for the other — so a suite cannot run the other's scenarios whatever you pass on the
command line.

```bash
mvn -Psmoke verify        # @smoke only, the default
mvn -Papi verify          # @api only
mvn -Pregression verify   # everything not tagged @wip, on the Surefire side
```

## Two things worth knowing before you change the POM

**Glue packages are per suite.** Cucumber runs `@BeforeAll` for every glue package a suite
loads, and it scans them recursively. If you start a container from a class in a package that
the smoke suite lists, the smoke suite starts that container too — and every test still passes
while the promise above quietly breaks. That is why `support` is a separate package from
`steps`, and why each suite lists its own `cucumber.glue`.

**Each suite writes its own message log.** `target/cucumber-messages.ndjson` for smoke,
`target/failsafe-cucumber-messages.ndjson` for the integration suite. Sharing one means the
second run overwrites the first.

## Configuration

Values resolve highest-first: system properties, environment variables,
`config/<env>.properties`, `config/default.properties`.

```bash
mvn -Psmoke verify -Dapi.base.url=https://staging.example
API_BASE_URL=https://staging.example mvn -Psmoke verify
```

The five keys and their variables are `env`/`ENV`, `web.base.url`/`WEB_BASE_URL`,
`api.base.url`/`API_BASE_URL`, `timeout.default.seconds`/`TIMEOUT_DEFAULT_SECONDS` and
`timeout.polling.millis`/`TIMEOUT_POLLING_MILLIS`.

## More

Trident's own documentation covers the reasoning behind all of this:
[test strategy](https://github.com/cozgur/trident/blob/main/docs/test-strategy.md),
[runbook for a failing test](https://github.com/cozgur/trident/blob/main/docs/runbook-failing-test.md),
and the [architecture decisions](https://github.com/cozgur/trident/blob/main/docs/adr/README.md).
