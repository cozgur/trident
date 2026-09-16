# 1. Maven over Gradle

## Status

Accepted.

## Context

Trident is consumed as an external Maven artifact. Consumer projects add a dependency, import
a BOM, and later generate a project from an archetype. The framework's own build system is
therefore visible to its users through the coordinates, the BOM and the archetype, not just
internally.

The team building and maintaining Trident is small, and the build is not the interesting part
of the project. What matters is that a contributor can clone the repository and run the suite
without first learning the build.

## Decision

Maven, with the reactor rooted at `trident-parent`.

The build is pinned end to end: every dependency and plugin version resolves through a named
property in the parent POM, and `maven-enforcer-plugin` runs at `validate` with
`requireMavenVersion [3.9.0,)`, `requireJavaVersion 21`, `banDuplicatePomDependencyVersions`
and `requireReleaseDeps` (release builds only).

The Maven Wrapper is committed — `mvnw`, `mvnw.cmd` and `.mvn/wrapper/` — pinned to Maven
3.9.9.

## Consequences

- Publishing to Maven Central and shipping an archetype are ordinary Maven tasks rather than
  plugin work.
- The lifecycle is fixed. Binding something unusual is harder than in Gradle; nothing in
  Phase 0 needed it.
- Builds are slower than an incremental Gradle build. For a suite of this size the difference
  is not worth a second build system.

### The Maven Wrapper

The wrapper is committed so that **Git and Java 21 are the only prerequisites** — the README
quickstart works with no system Maven at all, and every contributor and CI job runs the same
Maven version. This also lets `requireMavenVersion` stay at `[3.9.0,)` without turning a
contributor's older system Maven into a blocked build: the wrapper simply supplies a version
that satisfies the rule. `.gitignore` must never exclude `.mvn/`, or the guarantee is lost.

### `requireJavaVersion` means "21 or higher"

The rule is configured as `21`, which in enforcer semantics is a minimum, not an exact match.
This is deliberate. Trident is consumed as a library, and a library that refuses to build on a
newer JDK than its authors happened to use forces every consumer onto Trident's schedule.
Sources compile with `maven.compiler.release=21`, so the bytecode target is fixed regardless
of the JDK in use; CI runs Temurin 21. Pinning the rule to `[21,22)` would reject JDK 22+ for
no benefit.

## Alternatives rejected

**Gradle.** Faster incremental builds, a more expressive build language, and better suited to
unusual build logic. Rejected because Trident's build is conventional, because publishing and
archetype support are more direct in Maven, and because consumers are Maven projects — a
Gradle-built artifact would still be consumed through a POM, so the flexibility would buy
nothing where it is visible.

**Maven without the wrapper.** Simpler repository, one less thing to keep current. Rejected
because it makes a correctly-versioned system Maven a prerequisite, which contradicts the
five-minute quickstart and lets contributors build with versions that were never tested.

**Pinning `requireJavaVersion` to exactly 21.** Guarantees everyone builds on the same JDK.
Rejected for the reason above: it exports Trident's toolchain choice to every consumer.
