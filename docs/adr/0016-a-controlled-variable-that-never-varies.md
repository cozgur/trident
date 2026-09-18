# 16. A variable held constant across every measurement is invisible, not controlled

## Status

Accepted. Supersedes [ADR 0012](0012-rest-assured-bypassed-for-non-json-error-bodies.md).

## Context

Phase 1 found that REST Assured would not hand back a non-2xx response. It threw
`HttpResponseException` from the request itself, so a scenario asserting that a bad password is
rejected could not read the status it existed to check.

That was investigated properly, or so it looked. The measurements were:

| attempt | result |
|---|---|
| `accept(JSON)`, bare `get()` | throws |
| `accept(JSON)` + `.then().extract().response()` | throws |
| `accept(ANY)`, bare `get()` | throws |
| no `Accept` header at all | throws |
| `.then().statusCode(400).extract().response()` | throws |
| REST Assured 5.5.2 → 6.0.1 | throws |
| a plain `400` from a five-line Python server | throws |

Every row is accurate. The last one was treated as decisive: an independent server, no ParaBank
involved, same result — so the behaviour must be REST Assured's. `trident-api` grew a public
`ApiRequest` class wrapping the JDK HTTP client, and ADR 0012 recorded the conclusion.

The conclusion was wrong. Varying the one thing none of those seven rows varied:

| locale | result |
|---|---|
| `tr_TR` | throws `HttpResponseException: status code: 400` |
| `en_US` | returns status `400` and the body |

REST Assured lowercases its internal handler key without specifying a locale. Under Turkish
rules `I` lowercases to the dotless `ı`, so `FAILURE` becomes `faılure`, the registered failure
handler is never found, and the default one throws. Seven experiments, one machine, one locale.

CI never saw it, because CI runs under a neutral locale — which should itself have been a
question rather than a relief.

**The same defect had already been found, one checkpoint earlier.** Publishing to Maven Central
failed with:

```
waitUntil must be one of the following values [uploaded, valıdated, publıshed]
```

Dotless `ı`, diagnosed correctly, worked around by forcing the locale, and written into
`docs/releasing.md`. The connection to the REST Assured behaviour was not made, because the two
looked like different kinds of problem: one was a configuration rejection, the other an
exception from a request.

## Decision

The rule, stated so it can be applied rather than admired:

> **A variable held constant across every measurement is not controlled for. It is invisible.**

Convergent results are only evidence of convergence when the setups differ in the ways that
matter. Seven runs that agree because they share an unexamined condition are one run reported
seven times, and they feel more convincing than one run, which is what makes them dangerous.

Operationally, for this project:

- When several independent-looking attempts all fail the same way, the next question is not
  "what else can I try" but "what has every attempt had in common". Machine, locale, JDK, shell,
  network, clock.
- CI passing while local fails is a difference to explain before it is a relief. It is a free
  second environment and the cheapest way to find the shared condition.
- A conclusion that adds public API is worth one more attempt at disproof before it ships, not
  after.

## Consequences

- The test locale is pinned with `-Duser.language=en -Duser.country=US` as an `argLine`, in the
  reference implementation and in the project the archetype generates. It must be an `argLine`,
  not a `systemPropertyVariable`: `Locale.getDefault()` is fixed when the JVM starts. This is
  ordinary hygiene independently of the bug — a suite that passes in Istanbul and fails in
  Berlin is not a suite.
- `ApiRequest` and `ApiResponse` are removed. They existed only to work around one machine's
  locale, could not send a request body or read response headers, and would have been frozen by
  1.0.0 four days after being written.
- ADR 0012 keeps its measurement table and is marked superseded. Deleting it would remove the
  only record of how a careful-looking investigation reached a wrong answer, which is the part
  worth keeping.
- A consumer on a Turkish-locale machine who adds `trident-api` to an existing project, rather
  than generating from the archetype, still meets the underlying REST Assured bug. We cannot fix
  their build; the archetype sets the locale for everyone who starts from it.

## Alternatives rejected

**Record it as "the Turkish locale bites twice" and move on.** Accurate and useless to anyone
not writing Turkish-locale software. The locale is the instance; the method is the finding, and
the next instance will not involve a dotless i.

**Keep `ApiRequest` anyway, since it works.** It does work, and it would spare anyone on a
Turkish machine. Rejected because it is public API bought with a misdiagnosis: two ways to make
a request, one of them strictly weaker, maintained forever to paper over someone else's library
bug on one laptop.

**Treat it as a one-off and add no rule.** Rejected because it was not a one-off: the same
underlying defect had already appeared in a different tool in the same fortnight, and the reason
it was missed the second time was a habit of thought, not a gap in knowledge.
