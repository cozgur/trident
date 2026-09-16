# Working with a legacy application

Trident is developed against [ParaBank](https://github.com/parasoft/parabank), a banking demo
application from around 2005. This was a deliberate choice over a modern demo site, and it
shapes the framework more than any other decision.

## Why an old JSP application

A modern demo site is built to be automated. It has stable `data-testid` attributes, a JSON
API that matches its UI, and a reset endpoint. Writing tests against one proves you can use a
tool. It does not prove much else.

ParaBank has the properties that make real work hard:

- **A DOM you cannot rely on.** Generated table markup, ids that change with the data, layout
  driven by nested tables rather than semantic elements.
- **No test hooks.** Nobody added attributes for automation, because in 2005 nobody was going
  to automate it.
- **Two APIs that disagree.** SOAP and REST both exist, over the same data, with different
  shapes. Trident uses REST only; SOAP is a
  [non-goal](NON-GOALS.md).
- **Global state.** Administration settings and database resets affect every user at once.

Most software that needs testing looks more like this than like a demo site.

## We do not modify the application under test

This is the rule that follows from everything above, and it is the one worth defending.

It is tempting to add an id to a JSP, or a test-only endpoint, and be done. On this project we
could — the source is available. We do not, because in the jobs this framework is meant for
you usually cannot: the application is owned by another team, or another company, or it is a
vendor binary. A suite that only works after someone patches the product is a suite that will
not survive contact with a real client.

So the constraint is self-imposed and kept on purpose. What it forces: locators built from
what the page actually shows — visible text, labels, structural relationships — rather than
from attributes we wished were there. Phase 2 will implement this; today the framework has no
web layer, so treat this as the stated rule rather than as something the repository yet
demonstrates.

## Isolation without a reset

ParaBank offers `cleanDB` and `initializeDB`. They would make isolation trivial: wipe
everything between scenarios.

They are forbidden here. A global reset means exactly one thing can run at a time, forever. It
rules out parallel execution, it rules out two engineers running the suite at once, and it
rules out ever pointing the suite at a shared environment. The alternative costs more to write
and keeps all three options open: **each scenario creates the data it needs and touches
nothing else.** Its own customer, its own accounts, a unique identifier per run.

This is also how you test a production-like system you do not own, where no reset endpoint
exists and would not be given to you.

## Establishing ground truth with no specification

There is no specification for ParaBank's behaviour. In my experience this is the normal case,
not the exception, and the method that works is:

1. Observe the current behaviour and write it down.
2. Find out whether anyone depends on it. Usually someone does.
3. Only then decide whether it is correct.

When the application is simply wrong — and ParaBank has arithmetic that is — the test asserts
what the application does, with a comment saying it is wrong and a link to the issue. A test
that asserts the correct answer against a product that gives the wrong one is a permanently
red test, which teaches everyone to ignore red.

## What ParaBank is not

It is still a demo application, and the gap matters:

- **No real traffic.** One user, ours, doing one thing at a time.
- **No data volume.** A handful of accounts. Queries that would be slow against a real
  database are instant here.
- **No concurrency.** Race conditions between real users cannot appear.
- **No operational surface.** No deploys mid-test, no dependency outages, no clock skew.

Techniques this repository demonstrates transfer to a real system. The confidence a green run
provides here does not.
