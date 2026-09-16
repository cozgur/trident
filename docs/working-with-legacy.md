# Working with a legacy application

This is about the kind of application Trident is built to handle: one you did not write, cannot
change, and have no specification for. It is illustrated by
[ParaBank](https://github.com/parasoft/parabank), a banking demo from around 2005 that Trident
uses as its reference target. ParaBank is the example, not the subject.

## Why an old JSP application

A modern demo site is built to be automated: stable `data-testid` attributes, a JSON API that
matches its UI, a reset endpoint. Writing tests against one proves you can use a tool. It does
not prove much else.

ParaBank has the properties that make real work hard:

- **A DOM you cannot rely on.** Generated table markup, ids that change with the data, nested
  tables instead of semantic elements.
- **No test hooks.** Nobody added attributes for automation, because in 2005 nobody was going
  to.
- **Two APIs that disagree.** SOAP and REST both exist, over the same data, with different
  shapes. Trident uses REST only; SOAP is a
  [non-goal](NON-GOALS.md).
- **Global state.** Administration settings and database resets affect every user at once.

Most software that needs testing looks more like this than like a demo site.

**None of this reaches the framework.** ParaBank's container, fixtures, step definitions and
feature files live in `trident-demo-parabank`, which is never published. The framework modules
do not know it exists, and that is checked rather than asserted:

```bash
grep -riE "parabank|testcontainers|docker" trident-core/src trident-api/src trident-runner/src
```

CI runs it and the build fails on any match. That is what makes the reference target
replaceable: Phase 2 adds a second one, and the framework should not need a line changed.

## We do not modify the application under test

This is the rule that follows from everything above, and it is the one worth defending.

It is tempting to add an id to a JSP, or a test-only endpoint, and be done. Here we could — the
source is available. We do not, because in the jobs this framework is meant for you usually
cannot: the application belongs to another team, another company, or a vendor binary. A suite
that only works after someone patches the product will not survive contact with a real client.

The constraint is self-imposed and kept on purpose. What it forces: locators built from what
the page actually shows — visible text, labels, structural relationships — rather than from
attributes we wished were there. Phase 3 implements this; today there is no web layer, so treat
it as the stated rule rather than something the repository yet demonstrates.

## Isolation without a reset

ParaBank offers `cleanDB` and `initializeDB`. They would make isolation trivial: wipe
everything between scenarios.

They are forbidden here. A global reset means exactly one thing can run at a time, forever: no
parallel execution, no two engineers running the suite at once, no shared environment. The
alternative costs more to write and keeps all three open: **each scenario creates the data it
needs and touches nothing else.** Its own customer, its own accounts, a unique identifier per
run.

This is also how you test a production-like system you do not own, where no reset endpoint
exists and would not be given to you.

## Establishing ground truth with no specification

There is no specification for ParaBank's behaviour. In my experience that is the normal case,
not the exception, and the method that works is:

1. Observe the current behaviour and write it down.
2. Find out whether anyone depends on it. Usually someone does.
3. Only then decide whether it is correct.

When the application is simply wrong, the test asserts what it does, with a comment saying so
and a link to the issue. A test that asserts the correct answer against a product that gives
the wrong one is permanently red, which teaches everyone to ignore red.

## What ParaBank is not

It is still a demo application, and the gap matters:

- **No real traffic.** One user, ours, doing one thing at a time.
- **No data volume.** A handful of accounts. Queries that would crawl against a real database
  are instant here.
- **No concurrency.** Race conditions between real users cannot appear.
- **No operational surface.** No deploys mid-test, no dependency outages, no clock skew.

The techniques here transfer to a real system. The confidence a green run provides does not.
