# 12. REST Assured was never the problem: the locale was

## Status

Accepted. **Supersedes the original version of this record**, which drew the wrong conclusion
from correct measurements.

## Context

A scenario asserting that a login with the wrong password is rejected could not read the
response. REST Assured threw `HttpResponseException: status code: 400` from the request itself,
out of `HTTPBuilder.defaultFailureHandler`, before any assertion ran.

This was measured carefully and the measurements were sound: it happened with and without an
`Accept` header, with `.then().statusCode(400)`, across REST Assured 5.5.2 and 6.0.1, and
against a five-line Python server returning an ordinary 400. The conclusion drawn from them was
that REST Assured surfaces every non-2xx by throwing, and `trident-api` grew a public
`ApiRequest` class using the JDK HTTP client so that rejections could be asserted at all.

**The conclusion was wrong.** Every one of those measurements was taken on the same machine,
and the variable that mattered was never changed. Holding the request identical and varying only
the JVM locale:

| locale | result |
|---|---|
| `tr_TR` | throws `HttpResponseException` |
| `en_US` | returns status 400 and the body |

REST Assured lowercases its internal handler key without specifying a locale. Under Turkish
rules `I` lowercases to the dotless `ı`, so `FAILURE` becomes `faılure`, the registered failure
handler is never found, and the default one throws. It is the same defect this project had
already hit once, in the Central publishing plugin, whose error message printed
`[uploaded, valıdated, publıshed]` — and the connection was not made.

CI never saw any of it, because CI runs under a neutral locale.

## Decision

Tests run in a fixed locale. Both plugin blocks — in the reference implementation and in the
project the archetype generates — carry:

```xml
<argLine>-Duser.language=en -Duser.country=US</argLine>
```

It must be an `argLine` rather than a `systemPropertyVariable`: `Locale.getDefault()` is fixed
when the JVM starts.

`ApiRequest` and `ApiResponse` are **removed**. Both consumers assert rejections through the
ordinary request specification, which is what it was always able to do.

## Consequences

- There is one way to make a request, not two. A public class that existed only to work around
  one machine's locale is gone before 1.0 froze it.
- Suites no longer depend on the developer's locale, which is worth having on its own. A test
  that passes in Istanbul and fails in Berlin is not a test.
- A consumer on a Turkish-locale machine who adds `trident-api` to an existing project, rather
  than generating from the archetype, will hit the underlying REST Assured bug. We cannot fix
  their build; the archetype sets the locale for everyone who starts from it.
- The lesson is about method, not about locales: **a variable that is constant across all of
  your measurements is not controlled for, it is invisible.** Five configurations, two library
  versions and an independent server all agreed — and all ran on one machine. The agreement felt
  like convergent evidence and was a single untested assumption repeated five times.

## Alternatives rejected

**Keep `ApiRequest` as well, since it works.** It does work, and it would spare anyone on a
Turkish machine. Rejected because it is public API bought with a misdiagnosis: two ways to make
a request, one of which cannot send a body or read headers, maintained forever to paper over a
bug in someone else's library on one developer's laptop.

**Force the locale inside the framework, at class-load time.** A static initialiser in
`trident-api` calling `Locale.setDefault(Locale.ROOT)` would fix every consumer, generated or
not. Rejected as far too rude: a test library that silently changes the JVM's default locale
will eventually break somebody's date formatting assertion, and it would be very hard to find.

**Report it upstream and wait.** Worth doing and not a solution: the suite has to work now, and
pinning the locale is correct regardless of whether REST Assured ever changes.
