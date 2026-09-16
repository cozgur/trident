# 6. trident-bom is a standalone POM

## Status

Accepted.

## Context

`trident-bom` is Trident's public dependency contract. Downstream projects import it with
`<scope>import</scope>` and then declare Trident modules without versions.

A BOM was first written as an ordinary module inheriting from `trident-parent`. Maven builds
the effective model of an imported POM, which includes everything inherited from its parent.
The parent's `dependencyManagement` imports `cucumber-bom` and `junit-bom` and pins Owner,
AssertJ and SLF4J, so the BOM's effective `dependencyManagement` held 55 entries rather than
the intended 6.

This was confirmed, not assumed: a throwaway consumer importing the inheriting BOM could
declare a versionless `io.cucumber:cucumber-java` and Maven resolved it to `7.22.1` from the
parent's alignment.

The consequence is that Trident's internal third-party choices would become part of its
public contract. Raising `cucumber.version` in the parent — an internal decision about what
Trident builds against — would silently re-pin every downstream build that had imported the
BOM, including projects that never asked Trident to manage Cucumber for them.

## Decision

`trident-bom` declares no `<parent>`. It states its own `groupId`, `artifactId` and
`version`, and its `dependencyManagement` lists exactly the six sibling Trident artifacts at
`${project.version}`, which resolves against its own literal version.

It remains listed in the parent's `<modules>`. Reactor membership does not require
inheritance, so the BOM is still built, versioned and released alongside everything else.

Third-party alignment stays in `trident-parent`, where it governs how Trident itself builds
and reaches no consumer.

## Consequences

- An import of `trident-bom` manages the six Trident artifacts and nothing else. Consumers
  choose their own Cucumber, JUnit and AssertJ versions.
- The project version now appears literally in two places: the parent POM and the BOM. They
  must be kept in step. Release tooling must use `versions:set`, which updates non-inheriting
  modules in the reactor as well; a hand edit of the parent alone will produce a BOM that
  manages a version that was never built.
- The BOM does not inherit the parent's plugin configuration. It needs none for the
  lifecycle — it has no sources to compile, format or test — but the absence of inherited
  `pluginManagement` had a consequence that was not anticipated. A plugin goal invoked from
  the command line runs against every reactor project, and in a module with no
  `pluginManagement` Maven resolves the latest release from the network. Running
  `mvn com.diffplug.spotless:spotless-maven-plugin:apply` from the repository root picked
  spotless 3.10.2 inside the BOM while every other module used the pinned 2.44.5.

  The BOM therefore carries a minimal `pluginManagement` block pinning spotless to a literal
  `2.44.5`, with no execution and no configuration: the goal is a pinned no-op there. This is
  a second duplicated version string, and it is accepted for the same reason as the first —
  an unpinned plugin version resolved over the network is a worse problem than a duplicated
  literal, because it makes the build depend on what was released today. It must be bumped
  together with `spotless.version` in the parent.

  Excluding the module with `-pl '!trident-bom'` was rejected as a fix: it hides the symptom
  for one goal while leaving every other goal exposed.
- Trident may change its internal dependency versions without altering its public contract.

## Alternatives rejected

**BOM inheriting from `trident-parent`.** The conventional layout, and it keeps the version
in one place. Rejected because import carries the full effective `dependencyManagement`:
consumers silently received 49 third-party entries, and Trident could not bump an internal
dependency without re-pinning downstream builds. The duplicated version string is the
cheaper problem, and it is mechanically solvable with `versions:set`.

**Keeping the third-party alignment out of `trident-parent` so inheritance would be
harmless.** Rejected because the alignment exists to keep Trident's own modules building
against one consistent set of versions. Removing it to protect the BOM would trade a real
internal guarantee for a structural convenience.
