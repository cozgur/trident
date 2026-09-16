# Test strategy

What Trident tests, what it deliberately does not, and how we decide a feature has enough
tests. Written for anyone on the team, not only engineers.

## The layers, and what each one refuses to do

**Unit tests** check one class in isolation. They are fast, and they are where edge cases
belong — empty inputs, wrong types, boundary values. They do not tell us the system works:
every unit can be correct while the product is broken.

**API tests** send real HTTP requests to a running application and check the responses. They
tell us the business rules hold: a transfer moves money, a login rejects a wrong password.
They do not check that anything looks right, and they cannot catch a broken button.

**Web tests** (arriving in Phase 2) drive a browser. They tell us a user can complete a
journey end to end. They are the slowest and most fragile, so we write the fewest of them. The
rule: if a check can be made at the API layer, it is not a web test.

No layer here covers performance, security, accessibility, or behaviour under load. Those are
separate disciplines, and pretending a functional suite covers them is how teams get surprised
in production.

## Two suites, and why the fast one stays Docker-free

There are two commands. `./mvnw -Psmoke verify` needs only Git and Java 21. `./mvnw -Papi
verify` needs Docker and starts a real application in a container.

The split exists because a suite that is inconvenient to run does not get run. The moment a
developer must install Docker to check their change, the suite stops being something they use
and becomes something they wait for. So the fast suite is protected: it may never depend on a
container, and CI proves this by running it before Docker is even checked for. See
[ADR 0010](adr/0010-two-suites-surefire-and-failsafe.md).

## What "done" means for a feature

A feature is covered when all of these hold:

- If the main path breaks, a test fails.
- If the rule the feature exists to enforce is removed, a test fails.
- If a user is told something wrong — a bad error message, a wrong balance — a test fails.
- Someone reading the failure can tell what broke without opening the code.

None of these is a number.

**We do not set a coverage percentage target, and we will push back on one.** Coverage
measures which lines ran, not whether anything was checked — a suite can execute every line
and assert nothing. Worse, a target pushes people toward the easy uncovered code rather than
the risky covered code, so the number rises while the risk stays. Twelve tests someone can
explain beat four hundred that hit 90%.

## How long the suite may take

The smoke suite must finish in **under 60 seconds**. That figure is measured — today it takes
a few — and the budget exists to keep it there.

The API suite has no budget yet, because it has never run. Any number written today would be
invented, and an invented budget either gets quietly rewritten when reality disagrees or stays
wrong. The figure gets set the first time the suite runs in Phase 1, and recorded here.

> Budget: to be set. Container cold start is measured in CP 1.2; the suite total follows.

Exceeding a budget is a failure to fix, not a new normal. The first response is to delete or
merge scenarios, not to buy a bigger CI machine. A suite that creeps from two minutes to
twenty does it one reasonable-looking commit at a time.

## What this strategy is bad at

It will miss anything about *quality* that is not a rule. A page that loads but reads badly, a
flow that works but confuses people, a screen that is technically accessible and practically
unusable — every one of these passes.

It will also miss anything that only appears at scale or under concurrency. ParaBank is a demo
application with one user: us. Race conditions, slow queries against real data, and anything
involving load are invisible here. See
[working with legacy systems](working-with-legacy.md) for what else the target does not
represent.

Naming these is not an apology. It is so nobody reads a green build as a claim it never made.
