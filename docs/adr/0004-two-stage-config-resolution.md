# 4. Two-stage configuration resolution

## Status

Accepted.

## Context

Trident is consumed as an external Maven artifact. A consumer's build must be able to point
it at a different base URL or tighten a timeout without forking the framework, and a CI job
must be able to do the same from the environment alone.

That needs two things decided in order. First, which environment is active — a value that is
itself configurable and that the configuration files are named after. Second, the values for
that environment, layered so that a run-time override beats a file and a project file beats
Trident's shipped default.

Resolving both at once is circular: the source paths cannot be known until the environment
name is, and the environment name is itself an overridable setting.

## Decision

Resolution happens in two explicit stages.

**Stage 1** resolves the active environment: the `env` system property, then the `ENV`
environment variable, then the literal `local`. It lives in
`ConfigProvider.resolveEnvironment`, a pure package-private function taking both maps as
arguments so its precedence is unit-testable without a JVM fixture.

**Stage 2** builds the configuration around that name, highest precedence first: system
properties, environment variables, `classpath:config/${env}.properties`, then
`classpath:config/default.properties`. It lives in `ConfigLoader.load`, which receives an
already-resolved environment and applies no fallback of its own.

The mapping from property key to environment variable name is a closed table, not a
reflective transformation of key names. A key is readable from the environment only if it is
listed.

`TridentConfig` declares `@LoadPolicy(LoadType.MERGE)`.

`ConfigProvider.get()` resolves once and caches via an initialisation-on-demand holder, so
the cache is lazy and thread-safe with no mutable static state.

## Consequences

- Every key resolves through the same four layers, and the layer that supplied a value is
  predictable from the precedence list alone.
- `config/local.properties` deliberately overrides a single key. The other four must fall
  back to `config/default.properties`, so the fallback chain is observable in a test rather
  than assumed.
- Consumers override values by placing their own `config/*.properties` earlier on the
  classpath, or per-value with a system property or environment variable.
- There is no `reset()` or `reload()`. Configuration is an input to a test run, not something
  a test may change underneath other tests.
- Adding a key means editing three places: the interface, the closed environment-variable
  table, and `default.properties`. This is deliberate friction — it keeps the environment
  contract explicit.

### Owner behaviour discovered while implementing this

Owner's imported maps — the `Map` arguments to `create(...)` — outrank `@Sources` for value
lookup, but they **do not** feed the expansion of `${env}` inside the `@Sources` URIs. Owner
expands those from its *factory-level* properties.

Passing the resolved environment in the imported map alone is therefore not enough: `env()`
returns the right value while `${env}` stays unexpanded and `config/<env>.properties` is
never read. The symptom is silent — the configuration still loads, just entirely from
`default.properties`, so `defaultTimeoutSeconds()` returned `10` where `5` was expected.

`ConfigLoader.load` consequently creates a fresh `ConfigFactory.newInstance()` per call and
sets `env` on that `Factory`. The static `ConfigFactory.setProperty` would also work and is
what Owner's documentation shows, but it is JVM-global mutable state: it breaks test
isolation, leaks one caller's environment into the next, and will race once Phase 2 enables
parallel execution. The instance factory confines that state to a single `load` call. The
reason is recorded in a comment at the call site.

## Alternatives rejected

**Owner's default `FIRST` load policy.** `FIRST` stops at the first source that exists.
Because `classpath:config/${env}.properties` is listed first, any environment that ships a
file would make `config/default.properties` unreachable entirely — `local.properties`
defines one key, and the other four would resolve to nothing rather than falling back.
`MERGE` is what makes the two sources a fallback chain instead of a choice between them.

**A single-stage resolution reading everything at once.** Rejected as circular: the
`@Sources` paths are parameterised by the very value being resolved.

**Deriving environment variable names from property keys** (uppercase, dots to underscores).
Rejected because it makes the environment contract implicit and unbounded — every key ever
added would silently become settable from the environment, including ones that should not be.

**`ConfigFactory.setProperty` (the static factory).** Rejected as mutable static state; see
the consequence above.
