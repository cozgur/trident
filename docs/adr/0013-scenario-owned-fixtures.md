# 13. Scenarios own their fixtures

## Status

Accepted.

## Context

Every `@api` scenario needs a customer with accounts. ParaBank ships a demo customer and a
`cleanDB` endpoint, so the obvious design is: reset between scenarios, use the demo customer,
move on.

That design costs everything it saves. A global reset means exactly one thing can run at a
time — no parallel execution in Phase 3, no two engineers running the suite at once, no
pointing the suite at a shared environment. It is also unavailable on any system you do not
own, which is the kind of system this framework exists for.

## Decision

Each scenario creates the customer it needs and reads nothing another created.
`CustomerFactory` registers one per scenario; Picocontainer builds the factory and the
`ScenarioContext` per scenario, so no fixture outlives the scenario that made it. `cleanDB` and
`initializeDB` are never called, and CI greps the source trees for both names.

The framework does not know any of this: the factory, the container and the fixtures live in
`trident-demo-parabank`, which is never published.

## What ParaBank made us learn

The factory is longer than "POST a form" because the application had three surprises, each now
recorded at the line that handles it.

**Registration is session-bound.** A bare POST to `/parabank/register.htm` fails with
`Expected session attribute 'customerForm'`: the controller populates the form object on the
GET. The factory GETs the page first and carries the JSESSIONID with a `SessionFilter`.

**The username column holds twenty characters.** A 21-character name is rejected with *"This
username already exists"* — on a database where it certainly does not. Measured: 20 register,
21 do not. Names are nineteen characters.

**A failed registration returns 200.** ParaBank re-renders the form with the error, so the
status proves nothing. The factory asserts on the page's own words. Before it did, a rejected
registration surfaced three calls later as an unexplained `400` from login, which is how the
username limit stayed hidden for as long as it did.

One correction belongs in the record. The first diagnosis of that `400` blamed the `!` in the
generated password, on the theory that REST Assured percent-encodes path parameters and
ParaBank compares the raw segment. That was written into a comment before it was tested. It was
wrong: a password containing `!` logs in fine, raw or encoded, and the username length was the
only cause. The comment was removed rather than softened.

## Consequences

- Scenarios run in any order. Verified rather than asserted: the two isolation scenarios pass
  with the feature file's order reversed, and the ~0.9s difference between them follows
  whichever runs first, which identifies it as JVM warm-up rather than a data dependency.
- Phase 3 can enable parallel execution without revisiting fixtures.
- Each scenario pays for its own customer — about 0.1s once the JVM is warm, against a 8s
  container start. At this scale the fixtures are not the cost.
- ParaBank accumulates customers across a run. It does not matter: the container is discarded
  when the run ends, and reuse is disabled.
- A new scenario must create its own data. That is more typing than reusing a customer from the
  scenario above, and it is the whole point.

## Alternatives rejected

**`cleanDB` between scenarios.** Trivial to write and genuinely simpler. Rejected because it
serialises the suite forever and cannot be used against a shared or unowned environment — the
two things that matter most about where this framework is meant to run.

**One customer per suite, shared by every scenario.** Cheaper: one registration instead of
eight. Rejected because scenarios would then depend on each other's balances, and the first
transfer scenario would decide what the next one sees. The overdraft scenario alone would
leave the shared account at -9,999,684.50.

**A counter for unique usernames.** Simpler than a UUID and easier to read in the database.
Rejected because a counter is unique only within one JVM: it collides the moment two runs share
a ParaBank instance or the suite runs in parallel, which is precisely when isolation matters.
