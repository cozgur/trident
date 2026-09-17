# 11. The readiness probe targets the home page, not the layer under test

## Status

Accepted.

## Context

`ParaBankContainer` needs a wait strategy: a signal that the application is ready before the
first scenario runs. Two candidates were obvious.

Port-binding readiness was rejected on measurement. The container accepts TCP connections
0.2 seconds after start and serves its first request 5.3 seconds later. A suite that started
on the bound port would spend five seconds being refused.

The second candidate looked clearly right: probe the REST endpoint the `@api` scenarios
actually use, `/parabank/services/bank/accounts/12345`. It is the layer under test, a 200
there proves more than a rendered page, and an unknown account id returns 400, so the 200 is
not a catch-all.

It does not work. Polling only that endpoint, the container never became ready — 90 seconds,
no success. The container log said why:

```
user lacks privilege or object not found: ACCOUNT
```

The schema did not exist. Probing `/parabank/index.htm` instead returned 200 in about 6
seconds, and the REST endpoint answered 0.1 seconds after that.

**ParaBank builds its HSQLDB schema on the first request to the web application.** The wait
strategy is therefore not observing readiness. It is causing it.

The mechanism is visible in the response. On a fresh container the first `GET
/parabank/index.htm` answers `302` to `/parabank/initializeDB.htm`; every request after that
answers `200` directly. The application bootstraps itself, once, through a redirect, and
`Wait.forHttp` follows redirects — which is why the probe works and why a probe that skipped
the home page did not.

This is worth separating from the rule that scenarios never call `cleanDB` or `initializeDB`.
That rule is about resetting shared state underneath other tests. What happens here is the
application's own first-boot path on a container that is new in every run, before any scenario
exists, with no data to destroy. CI greps the source trees for those two endpoint names and
fails on a match; this ADR names them in prose, which is why the grep is scoped to sources
rather than the whole repository.

## Decision

Two steps, in this order.

The container waits on `GET /parabank/index.htm` returning 200, with a bounded three-minute
timeout. That request is what makes ParaBank initialise, so the wait is part of the startup
path rather than an observation of it.

The lifecycle then asks the REST API directly — one bounded poll of
`/parabank/services/bank/accounts/12345` — before it publishes the container's address. Only
when that answers 200 can a scenario start.

The comment at the call site says why the first step targets the home page, because the choice
looks wrong to anyone who has not seen the measurements and would otherwise "correct" it to the
REST endpoint alone, which never becomes ready.

## Consequences

- The suite starts reliably, and the REST layer is usable by the time the first scenario runs.
- The probe is load-bearing. Removing it, or narrowing it to a cheaper endpoint, does not just
  weaken a check — it can stop the application from initialising at all.
- The probe does not prove the REST layer answers, and for a while nothing else did either.
  This record used to claim the `@Before("@api")` hook covered that gap; it does not — the hook
  asks whether the container process is running, which is a different question. A pre-tag
  review caught the overclaim. The lifecycle now asks the REST API directly, once, after the
  home page has triggered initialisation and before the address is published, and fails with
  the last status it saw if the API never answers. The measured 0.1s gap between the page and
  the API is an observation, not a guarantee, and this is what turns it into one.
- The general lesson, and the reason this is an ADR rather than a comment: **a readiness probe
  against a lazily-initialised application is part of that application's startup path.**
  Choosing "the layer under test" as the probe is a sound instinct and was exactly wrong here.
  When a container is slow to become ready, the question is not only *what proves it is ready*
  but *what makes it ready*, and those can be different endpoints.
- This is a property of the application, not of Testcontainers, so it will recur with any
  target that initialises lazily. The image is pinned by digest, so the behaviour is pinned
  with it; a future image bump has to re-check this.

## Alternatives rejected

**Probe `/parabank/services/bank/accounts/12345`, the layer under test.** Rejected on
evidence: 90 seconds of polling never succeeded, because nothing had triggered schema
initialisation. This was the first implementation and the suite timed out at 180 seconds.

**Port-binding readiness.** Rejected on measurement: the port is bound at +0.2s and the first
request is served at +5.5s, so the wait would return 5.3 seconds too early. A JSP application
accepting connections says nothing about whether a servlet can answer.

**Probe only the home page, and trust that REST follows.** This was the first implementation
and it is no longer what the code does. Rejected on review: the REST layer answered 0.1 seconds
after the home page in every observed run, but every observed run is not every run, and the
suite had nothing that would tell the difference between a slow API and a broken one. The
lifecycle now waits on the home page — which is what triggers initialisation — and then asks
the REST API directly before publishing the address.

**Two Testcontainers wait strategies chained together.** The same effect, expressed in the
container definition. Rejected because the second condition is not really readiness of the
container: it is a question about the application that only makes sense after the first request
has been served. Keeping it in the lifecycle puts it in the order it actually happens and lets
the failure message carry the last status the API returned.
