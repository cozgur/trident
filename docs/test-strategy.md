# Test strategy

This describes what Trident tests, what it deliberately does not, and how we decide a feature
has enough tests. It is written to be read by anyone on the team, not only engineers.

## The layers, and what each one refuses to do

**Unit tests** check one class in isolation. They are fast and they are where edge cases
belong — empty inputs, wrong types, boundary values. They do not tell us the system works:
every unit can be correct while the product is broken.

**API tests** send real HTTP requests to a running application and check the responses. They
tell us the business rules hold: a transfer moves money, a login rejects a wrong password.
They do not check that anything looks right, and they cannot catch a broken button.

**Web tests** (arriving in Phase 2) drive a browser. They tell us a user can complete a
journey end to end. They are the slowest and the most fragile, so we write the fewest of them.
A rule we hold to: if a check can be made at the API layer, it is not a web test.

What no layer here does: performance, security, accessibility, and behaviour under real load.
Those are separate disciplines, and pretending a functional suite covers them is how teams get
surprised in production.

## Two suites, and why the fast one stays Docker-free

There are two commands. `./mvnw -Psmoke verify` needs only Git and Java 21, and finishes in
seconds. `./mvnw -Papi verify` needs Docker and starts a real application in a container.

The split exists because a suite that is inconvenient to run does not get run. The moment a
developer must install Docker before checking their change, the suite stops being something
they use and becomes something they wait for. So the fast suite is protected: it may never
acquire a dependency on a container, and CI proves this by running it before Docker is even
checked for. See [ADR 0010](adr/0010-two-suites-surefire-and-failsafe.md).

## What "done" means for a feature

A feature is covered when we can answer yes to all of these:

- If the main path breaks, a test fails.
- If the rule the feature exists to enforce is removed, a test fails.
- If a user is told something wrong — a bad error message, a wrong balance — a test fails.
- A person reading the failure can tell what broke without opening the code.

Notice that none of these is a number.

**We do not set a coverage percentage target, and we will push back on one.** Coverage
measures which lines ran, not whether anything was checked. A suite can execute every line and
assert nothing. Worse, a percentage target changes behaviour in the wrong direction: people
write tests for the easy uncovered code rather than the risky covered code, and the number
goes up while the risk stays. We would rather have twelve tests someone can explain than four
hundred that hit 90%.

## How long the suite may take

The smoke suite must finish in **under 60 seconds**. The API suite must finish in **under 10
minutes**, including the container's cold start.

When a suite exceeds its budget, that is treated as a failure to fix, not a new normal. The
first response is to delete or merge scenarios, not to buy a bigger CI machine. A suite that
creeps from two minutes to twenty does it one reasonable-looking commit at a time.

## What this strategy is bad at

It will miss anything about *quality* that is not a rule. A page that loads correctly but
reads badly, a flow that works but confuses people, a screen that is technically accessible
and practically unusable — every one of these passes.

It will also miss problems that only appear at scale or under concurrency. ParaBank is a demo
application with one user: us. Race conditions between real users, slow queries against real
data volumes, and anything involving load are invisible here. See
[working with legacy systems](working-with-legacy.md) for what else the target does not
represent.

Naming these is not an apology. It is so nobody reads a green build as a claim it never made.
