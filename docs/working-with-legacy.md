# Working with a legacy application

This is about the kind of application Trident is built to handle: one you did not write, cannot
change, and have no specification for. It is illustrated by
[ParaBank](https://github.com/parasoft/parabank), a banking demo from around 2005 that Trident
uses as its reference target. ParaBank is the example, not the subject.

## Why an old JSP application

A modern demo site is built to be automated: stable `data-testid` attributes, a JSON API that
matches its UI, a reset endpoint. Tests against one prove you can use a tool and little else.

ParaBank has the properties that make real work hard:

- **A DOM you cannot rely on.** Generated table markup, ids that change with the data, nested
  tables instead of semantic elements.
- **No test hooks.** Nobody added attributes for automation; in 2005 nobody was going to.
- **Two APIs that disagree.** SOAP and REST over the same data, in different shapes. Trident
  uses REST only; SOAP is a [non-goal](NON-GOALS.md).
- **Global state.** Administration settings and database resets affect every user at once.

Most software that needs testing looks more like this than like a demo site.

**None of this reaches the framework.** ParaBank's container, fixtures, steps and feature files
live in `trident-demo-parabank`, which is never published. The framework modules do not know it
exists, and that is checked rather than asserted:

```bash
grep -riE "parabank|testcontainers|docker" trident-core/src trident-api/src trident-runner/src
```

CI runs it and the build fails on any match. That is what makes the target replaceable: Phase 2
adds a second one, and the framework should not need a line changed.

## We do not modify the application under test

This is the rule that follows from everything above, and it is the one worth defending.

It is tempting to add an id to a JSP, or a test-only endpoint, and be done. Here we could — the
source is available. We do not, because in the jobs this framework is meant for you usually
cannot: the application belongs to another team, another company, or a vendor binary. A suite
that only works after someone patches the product will not survive a real client.

The constraint is self-imposed and kept on purpose. It forces locators built from what the page
shows — visible text, labels, structural relationships — rather than attributes we wished were
there. The web suite now does exactly that, and the strategy, the evidence behind it and the
test that enforces it are in
[ADR 0017](adr/0017-locator-strategy-for-an-unmodifiable-ui.md). ParaBank's login form turned
out to be the hard case: two inputs with no id, no `aria-label`, and labels with no `for`
attribute, so neither field has an accessible name at all.

## Isolation without a reset

ParaBank offers `cleanDB` and `initializeDB`. They would make isolation trivial: wipe
everything between scenarios.

They are forbidden here. A global reset means exactly one thing can run at a time, forever: no
parallel execution, no two engineers running the suite at once, no shared environment. The
alternative keeps all three open: **each scenario creates the data it needs and touches nothing
else** — its own customer, its own accounts, a unique identifier per run. It is also how you
test a production-like system you do not own, where no reset endpoint exists and would not be
given to you.

## Establishing ground truth with no specification

There is no specification for ParaBank's behaviour. In my experience that is the normal case,
not the exception, and the method that works is:

1. Observe the current behaviour and write it down.
2. Find out whether anyone depends on it. Usually someone does.
3. Only then decide whether it is correct.

When the application is simply wrong, the test asserts what it does, with a comment saying so
at the assertion. A test that asserts the correct answer against a product that gives the wrong
one is permanently red, which teaches everyone to ignore red. We do not file issues upstream:
ParaBank is a demo target we do not own, and chasing its defects is not this project's job.

Two worked examples, both measured. Registering a 21-character username fails with **"This
username already exists"** on a database where it certainly does not; the real cause is a
20-character column, since 20 register and 21 do not. And ParaBank has no overdraft protection:
a transfer far beyond the balance answers `200 Successfully transferred` and leaves the account
at -9,999,684.50.

Neither is worked around quietly. The factory generates 19-character names, and a scenario
asserts the overdraft as it happens — each with a comment at the line saying what the
application claims and what is actually true. If the overdraft is ever fixed that scenario
fails, and the fix is to rewrite it as a rejection, not to relax it.

## What ParaBank is not

It is still a demo application, and the gap matters:

- **No real traffic.** One user, ours, one thing at a time.
- **No data volume.** A handful of accounts; queries that would crawl on real data are instant.
- **No concurrency.** Race conditions between real users cannot appear.
- **No operational surface.** No deploys mid-test, no dependency outages, no clock skew.

The techniques here transfer to a real system. The confidence a green run provides does not.
