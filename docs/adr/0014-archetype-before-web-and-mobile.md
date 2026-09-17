# 14. The archetype comes before the web and mobile layers

## Status

Accepted.

## Context

Trident's central claim is that adopting it means generating a project and writing feature
files, not forking a template and maintaining a copy of the plumbing. Every structural decision
so far serves that claim: the BOM, the standalone publishable modules, the split that keeps
target knowledge out of the framework
([ADR 0008](0008-framework-modules-know-no-target.md)).

Through Phase 1 the claim was undemonstrated. Nobody had generated a project. The only consumer
was `trident-demo-parabank`, which lives in this reactor, inherits this parent, and resolves
Trident from the local repository — so it could pass while a real outside project could not
even start.

The original roadmap put the archetype after the web and mobile layers, which would have left
the claim unproven for two more phases.

## Decision

The archetype moves to Phase 2, ahead of `trident-web` and `trident-mobile`. Phase 2's order
is archetype, then Maven Central, then `trident-showcase` — each step making the next one
honest: the showcase is *generated* from the archetype and resolves Trident from Central,
because a hand-written showcase against a local install proves nothing.

The archetype generates a standalone project with no parent, resolving Trident through
`trident-bom`, with both suites, the per-suite tag composition and the separate glue packages
already wired. Those three were what Phase 1 got wrong first
([ADR 0010](0010-two-suites-surefire-and-failsafe.md)); a consumer must not have to rediscover
them.

## Consequences

- The claim becomes testable, and is tested: generation into a directory outside this
  repository, then a passing suite with no edits, with a second project at a different package
  and prefix to prove the template is parameterised rather than merely working for the values
  it was written with.
- Every framework decision now has a consumer-facing shape that must be generated correctly.
  A change to the suite base class, the tag composition or the glue split is also a change to
  the archetype, and the archetype's verification is what catches it.
- The web and mobile layers arrive later than first planned. That is the cost, and it is worth
  paying: each layer accumulates target-specific assumptions, and unpicking two layers' worth
  of them to fit a generated project is more expensive than generating the project first and
  building the layers against it.
- The generated project pins its own plugin and third-party versions, because it has no parent
  to inherit them from. Those numbers live in the archetype template and must be reviewed when
  Trident's own move.
- The Trident version the template resolves is filtered in from the archetype's own version at
  build time, so it cannot drift from the release it ships with.

## Alternatives rejected

**A documented copy-paste template.** A section in the README showing the POM, the suite
classes and the properties file, for adopters to copy. Cheapest to write and it needs no new
module. Rejected because it drifts silently: the framework changes, the document does not, and
nobody finds out until an adopter copies something that stopped working a release ago. An
archetype is code, so it is built, and its output is executed — a stale archetype fails a
build, while a stale document fails a person.

**A parent POM consumers inherit from.** Trident could publish a parent carrying the plugin
configuration, the profiles and the dependency management, so a consumer inherits all of it and
writes almost nothing. Tempting, and common. Rejected for two reasons. It couples the
consumer's build to ours: our plugin choices, our lifecycle bindings and our version bumps
become theirs, whether or not they suit the project. And a POM has exactly one parent — a
consumer with a corporate parent, which is most of them, simply could not use it. The BOM gives
the part worth sharing, which is version alignment, without taking the build.

**Waiting until the web layer exists so the archetype can generate web scaffolding too.**
Rejected because it inverts the dependency: the archetype is how we find out whether the
framework is adoptable, and finding out later means more to unpick. The archetype generates
what exists today, and grows when the layers do.
