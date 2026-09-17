# 12. The rejection scenario bypasses REST Assured

## Status

Accepted.

## Context

One `@api` scenario asserts that a login with the wrong password is rejected. ParaBank answers
it correctly: `400`, `Content-Type: text/plain`, body `Invalid username and/or password`. The
scenario should assert both the status and the message.

It could not. REST Assured never returned the response — it threw
`io.restassured.internal.http.HttpResponseException: status code: 400` from the request itself,
out of `HTTPBuilder.defaultFailureHandler`, before any assertion could run.

That is not REST Assured's documented behaviour, so it was measured rather than assumed:

| attempt | result |
|---|---|
| `accept(JSON)`, bare `get()` | throws |
| `accept(JSON)` + `.then().extract().response()` | throws |
| `accept(ANY)`, bare `get()` | throws |
| no `Accept` header at all | throws |
| `.then().statusCode(400).extract().response()` | throws |
| REST Assured 5.5.2 → 6.0.1 | throws |
| **a plain `400` from a five-line Python server** | **throws** |

The last row is the one that settles it. The response had a normal reason phrase, a
`Content-Length`, and `text/plain` — nothing unusual — and REST Assured threw identically. So
this is not a ParaBank quirk, not a content-type problem, and not fixed by the current version.
Every non-2xx in this environment arrives as an exception.

## Decision

That one step issues its request with the JDK's `java.net.http.HttpClient`, records the status
and body in the scenario context, and the assertion step reads both from there.

Every other request in the suite goes through `ParaBankApi`, which is built from the
framework's `RequestSpecFactory` — including the HTML registration form, which needs a
different content type but the same base URI and timeouts. The bypass is one step wide and
commented at the line, with the measurements above summarised, so nobody "simplifies" it back.

That was not true when this record was first written. A pre-tag review found six call sites in
`CustomerFactory` and `CustomerSteps` still using a bare `given()`, quietly opting out of the
configured timeouts and the failure logging while this ADR claimed otherwise. They were routed
through `ParaBankApi`, and the JDK client in the bypass gained the same timeout, which it had
been missing.

## Consequences

- The scenario asserts what it was written to assert: status `400` and the message text.
- One step in the suite does not exercise the framework's request specification. That is a real
  loss and the reason this is scoped to a single step rather than adopted as a pattern. It
  carries the configured timeout explicitly so that it does not also opt out of that.
- The JDK client is standard library, so nothing was added to the dependency tree.
- Any future scenario asserting a 4xx or 5xx hits the same wall. When the second one appears,
  the right move is a small helper in the demo module rather than a second copy of this code —
  but it is not worth building for one caller.
- If a REST Assured release fixes this, the scenario should move back and this ADR should be
  superseded. The measurement table is here so that can be re-checked in minutes.

## Alternatives rejected

**Assert on the thrown exception.** `assertThatThrownBy(...).hasMessageContaining("status code:
400")` keeps everything inside REST Assured and is three lines shorter. Rejected because it
asserts on a library's exception text — a string the library is free to change in a patch
release — and because it cannot reach the body at all, so the scenario would silently stop
checking that ParaBank says *why* it rejected the login.

**Drop the status-code assertion and check only that the call failed.** Simplest of all.
Rejected because "it threw" is true of a connection refused, a timeout, a DNS failure and a
500. The scenario exists to distinguish a rejection from a breakage, and that distinction is
exactly the status code.

**Downgrade REST Assured until a version behaves.** Rejected as unbounded: 5.5.2 and 6.0.1 both
throw, the behaviour is not version-specific in the range we would accept, and pinning an old
version to work around an unexplained local behaviour trades one unknown for an older one.
