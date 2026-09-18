# 15. trident-api ships a JSON serialiser

## Status

Accepted.

## Context

`trident-api` gives consumers a configured REST Assured `RequestSpecification`. Through Phase 1
it was exercised against exactly one application, ParaBank, whose API takes query parameters
and form parameters and never a JSON request body.

The first outside consumer — `trident-showcase`, generated from the archetype and testing
Conduit — failed on its very first step:

```
java.lang.IllegalStateException: Cannot serialize object because no JSON serializer found in
classpath. Please put Jackson (Databind), Gson, Johnzon, or Yasson in the classpath.
```

REST Assured parses a JSON *response* on its own, but serialising a request body needs a JSON
provider that REST Assured does not depend on. `trident-api` shipped `rest-assured` and nothing
else, so a consumer's classpath had no provider at all.

An API testing module that cannot send a JSON body is not finished. The reason nobody noticed
is the whole point of this record: the only target in the repository never needed one.

## Decision

`trident-api` depends on `com.fasterxml.jackson.core:jackson-databind` at compile scope, so
every consumer gets a working serialiser transitively.

Jackson rather than Gson or Johnzon because REST Assured looks for it first and it is the
provider most consumers already have, so it is the choice least likely to conflict with what a
project is already using.

A unit test in `trident-api` asserts the provider is reachable. It looks like a tautology and
is not: it fails if the dependency is ever removed, in Trident's own build, rather than in
somebody else's project on their first POST.

## Consequences

- A consumer can POST a JSON body with no extra dependency, which is what an API testing module
  should have done from the start.
- Trident now has an opinion about JSON binding. That is a real cost: a consumer already using
  Gson gets Jackson on the test classpath too. It is scoped to test code and it is the
  conventional choice, so the cost is small and the alternative — every consumer discovering
  this from a stack trace — is worse.
- The fix arrived as `0.2.2`, and `trident-showcase` was rebuilt against it rather than working
  around it locally. A workaround in the showcase would have hidden exactly what the showcase
  exists to measure.
- The general lesson: **a framework developed against one target grows that target's shape.**
  ParaBank took form parameters, so the framework never learned to send a body. Nothing in the
  code said "ParaBank" — the CI grep was clean the whole time — and the assumption still leaked
  in through what the framework did *not* do. A second, deliberately different target is what
  surfaced it, which is the argument for the showcase existing at all.

## Alternatives rejected

**Let the consumer add Jackson.** One line in their POM, and the framework keeps no opinion
about JSON binding. Rejected because the failure arrives as a stack trace at the first POST,
names four libraries without saying which to choose, and every consumer pays the same tax for
the same reason. A module called `trident-api` that cannot send an API request body is
incomplete, not neutral.

**Add it to the archetype's generated POM instead.** Keeps `trident-api` free of the
dependency, and the generated project works. Rejected because it only helps projects generated
from the archetype: a consumer who adds `trident-api` to an existing project hits the same wall
with nothing to tell them why.

**Wrap REST Assured so the framework serialises the body itself.** Then the provider is an
implementation detail we control. Rejected as far more than the problem requires, and it would
put a hand-written serialisation layer between consumers and a library they already know.
