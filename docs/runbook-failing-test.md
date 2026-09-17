# Runbook: a test failed

It is 17:40 and CI is red. Work through this in order.

## 1. Is it your change, or is `main` already red?

Check before you debug anything.

```bash
gh run list --branch main --limit 5
```

If `main` is red, the failure is not yours. Say so in the PR, link the run, and stop. Fixing
someone else's breakage from inside your PR makes both changes harder to review and hides who
broke what.

If `main` is green, it is yours. Continue.

## 2. Read the execution gate output

CI runs a step called *Scenario-execution gate* for each suite. It prints this before it
decides anything:

```
scenarios started     = 1
scenarios finished    = 1
scenarios passing     = 1
executed tests        = 1
```

- **started** — Cucumber began the scenario.
- **finished** — it reached the end. A started-but-unfinished scenario means the JVM died
  mid-run; look for an error above, not for an assertion failure.
- **passing** — every step and hook reported `PASSED`. This is the only number that means work
  happened.

If `passing = 0` the build fails **even when nothing reported a failure**. A green build that
executed no scenarios is treated as a failure here, because it is indistinguishable from a
suite that was silently filtered out of existence. The reasoning, and the three ways we found
a build to lie about this, are in
[ADR 0007](adr/0007-pinning-surefire-and-verifying-execution.md).

If the gate failed but every scenario passed, the problem is the wiring, not the test.

## 3. Reproduce it locally

```bash
./mvnw -Psmoke verify          # fast suite, no Docker
./mvnw -Papi verify            # API suite, needs Docker
```

To run one scenario, tag it and filter on the tag:

```bash
./mvnw -Psmoke verify -Dtrident.tags='@smoke and @your-tag'
```

The tag expression is composed per suite, so a tag alone will not pull a scenario into the
wrong suite.

If it passes locally and fails in CI, download the `test-reports` artifact from the failed
run. The Cucumber message log in it is the full record of what executed.

## 4. If it is flaky

A test that fails once and passes on re-run is a flake. Do not file a bug on the application
yet. In my experience the first flake is usually the suite's fault, not the product's — a
missing wait, a shared fixture, an assumption about ordering. Re-run once. If it fails
differently the second time, it is the suite.

## 5. Quarantine

You may tag a scenario `@wip` to take it out of `regression`. That is the whole mechanism.

- **Who decides:** whoever is blocked. You do not need permission to quarantine. You do need
  to say so in the PR.
- **What it obliges you to do:** open an issue the same day, linked from a comment on the
  scenario, naming what is broken and who is looking at it.
- **Deadline: 14 days.** After that the scenario is deleted, not re-quarantined.

That deadline is the point. A quarantine with no expiry is deletion with extra steps — the
test still runs nowhere, but everyone gets to feel it is temporary. If the scenario is worth
keeping, fourteen days is enough to fix it. If it is not, deleting it is honest.

## 6. When to delete instead of fix

Delete the test when:

- It tests something the product no longer does.
- It duplicates a test at a lower layer that is faster and more precise.
- Nobody can say what it is for, including the person who wrote it.
- It has been quarantined twice for the same reason.

A deleted test is a decision. A permanently skipped test is a decision nobody made, sitting in
the repository looking like coverage.
