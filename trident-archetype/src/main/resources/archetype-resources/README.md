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
- `@web` — runs under Failsafe in `${prefix}WebIT`, only under `-Pweb`. Needs a browser.

The tag expression is composed per suite in `pom.xml` — `@smoke and (...)`, `@api and (...)`,
`@web and (...)` — so a suite cannot run another's scenarios whatever you pass on the command
line.

```bash
mvn -Psmoke verify        # @smoke only, the default
mvn -Papi verify          # @api only
mvn -Pweb verify          # @web only, in a browser
mvn -Pregression verify   # everything not tagged @wip, on the Surefire side
```

## The browser suite

**It does not run unless you ask for it.** `mvn verify` activates the smoke profile, which
skips it, so this project is green on a machine with no browser. That is deliberate: a
generated project should build the minute it is generated, wherever it is generated.

Turn it on with `mvn -Pweb verify`. Selenium Manager resolves the browser and its driver, so
there is nothing to install beyond the browser itself. Headless Chrome is the default:

```bash
mvn -Pweb verify                              # headless Chrome
mvn -Pweb verify -Dbrowser.headless=false     # watch it
mvn -Pweb verify -Dbrowser=firefox            # or another browser
```

The generated scenario renders its own page from a data URL, so it needs no server. If it
passes, your browser and driver work. Point `ExamplePage.open()` at `config.webBaseUrl()` when
you have an application to drive.

**Locators live in page objects, never in steps.** `ExamplePage` shows the ladder Trident
recommends: accessible name first, then a Selenium relative locator, then a structural XPath
from an anchor, and CSS or id last. Absolute XPaths and positional indexes are banned outright
— both pin a position rather than a thing. The reasoning, and a test that enforces it, are in
Trident's `docs/adr/0017-locator-strategy-for-an-unmodifiable-ui.md`.

**Every page object declares when it is loaded.** `LoadablePage` waits for that condition
before any interaction, which is the guard against a page that has arrived and is not finished
— an empty table, a dropdown still being filled. Pick a condition that is false on a
half-rendered page.

## Two things worth knowing before you change the POM

**Glue packages are per suite.** Cucumber runs `@BeforeAll` for every glue package a suite
loads, and it scans them recursively. If you start a container from a class in a package that
the smoke suite lists, the smoke suite starts that container too — and every test still passes
while the promise above quietly breaks. That is why `support` is a separate package from
`steps`, and why each suite lists its own `cucumber.glue`.

**Each suite writes its own message log and its own reports directory.**
`target/cucumber-messages.ndjson` for smoke, `target/failsafe-cucumber-messages.ndjson` for
api, `target/web-cucumber-messages.ndjson` for web, and a matching reports directory each.
Sharing either means one run overwrites another, and it also costs you the ability to say which
layer failed. The two Failsafe executions carry separate `summaryFile` values for the same
reason: the `verify` goal reads the summary its own `integration-test` goal wrote.

## Reporting

This project generates no report, and that is a choice rather than an omission: which report a
team uses is a decision the framework should not make for you. The suites already write
machine-readable output — a Cucumber message log per suite, plus the JUnit XML each plugin
produces — which most tools can read.

To add [Allure](https://allurereport.org/), which is what Trident's own reference suite uses:

1. Add `io.qameta.allure:allure-cucumber7-jvm` as a test dependency.
2. Append `io.qameta.allure.cucumber7jvm.AllureCucumber7Jvm` to each suite's `cucumber.plugin`
   in `pom.xml`, and set `allure.results.directory` alongside it.
3. Add the `io.qameta.allure:allure-maven` plugin and run `mvn allure:report`.

Attach evidence on failure and nothing on success — a report where every scenario carries a
screenshot is a report nobody opens. Trident's reference implementation shows the whole
arrangement, including the hook ordering that decides whether a screenshot is taken before or
after the browser closes:
<https://github.com/cozgur/trident/tree/main/trident-demo-parabank>.

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
