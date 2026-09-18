# Contributing

This suite is meant to be useful to you, not to be a gate you negotiate with. If it is
slowing you down or telling you things you cannot act on, that is a bug in the suite. Say so.

## Running tests locally

```bash
./mvnw -Psmoke verify
```

Git and Java 21 are all you need. Maven comes from the wrapper in this repository. This is the
one you are expected to run before opening a PR.

```bash
./mvnw -Papi verify
```

This runs the API scenarios against a container holding the target application, and needs
Docker. In this repository the target is ParaBank. You are welcome to run it, but you are not
expected to. CI runs it on every PR, and CI is the right place for it:
it takes longer and it will not fail for reasons that have anything to do with your laptop.

If you only change production code and the smoke suite is green, open the PR.

## Adding a scenario

Feature files live in your project's `src/test/resources/features/`. In this repository that
is `trident-demo-parabank`, the reference implementation; in a consumer project it is the
project the archetype generated. Two tags decide where a scenario runs, and every scenario
needs exactly one of them:

- `@smoke` — runs under Surefire, no Docker, must stay fast.
- `@api` — runs under Failsafe against a real container.

A scenario with neither tag runs nowhere. That is deliberate; see
[ADR 0010](docs/adr/0010-two-suites-surefire-and-failsafe.md).

**A scenario owns its data.** It creates the customer, account, or record it needs, and it
never reads something another scenario created. This is the rule that lets scenarios run in
any order, and later in parallel, without anyone having to think about it. If you find
yourself wanting to reuse the customer from the scenario above, write a second customer
instead — it costs one line and buys you a test that never fails because someone reordered
the file.

Do not reach for a global reset endpoint. ParaBank has `cleanDB` and `initializeDB`, most
applications have something like them, and they would all be easier. They also make every
scenario depend on being the only thing running.

## When a test fails on your PR

Read [the runbook](docs/runbook-failing-test.md). It is written for exactly that moment.

The short version: check whether the same test is failing on `main`. If it is, it is not
yours.

## Code style

Spotless enforces it. Do not argue with the formatter, and do not reformat by hand:

```bash
./mvnw com.diffplug.spotless:spotless-maven-plugin:apply
```

This runs unfiltered from the repository root. `spotless:check` runs as part of `verify`, so
a PR with unformatted code fails before anyone reviews it.

## What this suite will not ask of you

Drawing this line is the point of this section. It is a commitment, not a courtesy.

**You will not be asked to maintain locators.** Page objects and
selectors belong to whoever owns the suite. If a UI change breaks a selector, that is our
repair, not a task added to your PR.

**You will not be asked to write Gherkin.** If a bug needs a regression test, describe the
bug in a sentence and we will write the scenario. Gherkin is a communication format, and it
only earns its overhead when someone outside the team reads it.

**You will not be asked to wait on a container.** The suite you run before pushing is
Docker-free and finishes in seconds. Keeping it that way is a design constraint, not an
accident — see [the test strategy](docs/test-strategy.md).

**You will not be asked to fix a test you did not break.** If a test is flaky, tell us and
move on. Chasing someone else's flake is how a suite becomes something people route around.

## What we ask instead

Tell us when a test's failure message did not tell you what was wrong. That is the most
useful feedback this suite can get, and it is the one thing we cannot see from the inside.
