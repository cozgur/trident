# Releasing

Trident publishes to Maven Central from the `dev.ozgurcetintas` namespace, verified by a DNS
TXT record on `ozgurcetintas.dev`. A release is a tag push; nothing else publishes.

## The sequence

**1. Set the version.**

```bash
./mvnw versions:set -DnewVersion=0.2.1 -DprocessAllModules=true -DgenerateBackupPoms=false
```

`-DprocessAllModules=true` is required, not a refinement. `trident-bom` has no parent, and
without the flag `versions:set` leaves it behind while everything else moves — publishing a BOM
that manages coordinates which were never built. This was measured; see
[ADR 0006](adr/0006-standalone-bom.md).

Check all nine, not the parent alone:

```bash
grep -rn "<version>" pom.xml */pom.xml | grep -v '\${' | grep -v '<parent>'
```

**2. Commit, tag, push.**

```bash
git commit -am "Release 0.2.1"
git tag -a v0.2.1 -m "..."
git push && git push origin v0.2.1
```

The tag push starts `release.yml`. It runs the same checks `main` gets — the leak grep, both
suites, the execution gate — and only then deploys. A tag is not a reason to publish something
that would have failed on a branch.

**3. Watch the run.**

```bash
gh run watch --exit-status
```

**4. Check the Portal.**

[central.sonatype.com](https://central.sonatype.com) → *Deployments*. A deployment moves
through `PENDING` → `VALIDATING` → `PUBLISHING` → `PUBLISHED`. The workflow sets
`autoPublish=true` and `waitUntil=published`, so the job stays red until Central says it is
done — you do not have to remember to come back and click anything.

Artifacts appear in the search index some minutes after `PUBLISHED`, and on
`repo1.maven.apache.org` sooner. The gap is normal and is not a failed release.

**5. Set the next development version.**

```bash
./mvnw versions:set -DnewVersion=0.3.0-SNAPSHOT -DprocessAllModules=true -DgenerateBackupPoms=false
git commit -am "Back to snapshot"
git push
```

## What gets published

Eight of the nine modules. `trident-demo-parabank` is the reference implementation, not a
product: it sets `maven.deploy.skip` and is named in the publishing plugin's `excludeArtifacts`
as well. Two mechanisms for one rule, because publishing a demo is not something you want to
find out about afterwards.

## When a publish fails

**Validation rejected it.** Central names the field. The usual causes are a missing `name`,
`description`, `url`, `licenses`, `developers` or `scm` on a module — remember `trident-bom`
does not inherit and carries its own copy — or a missing sources or javadoc jar. Fix, set a new
patch version, tag again. Do not reuse the version.

**Signing failed.** Check that `GPG_PRIVATE_KEY` is the full armoured block including both
header and footer lines, and that the key has not expired. The current key expires
**2028-09-16**.

**It uploaded but did not publish.** A deployment sitting in `VALIDATED` can be dropped from
the Portal. Do that rather than leaving it: a stale deployment blocks the same coordinates.

## Rolling back

You cannot. **Maven Central is immutable — a published version can never be replaced or
removed.** This is the reason the first release was `0.2.1` rather than `0.3.0`: the first
publish tests the pipeline, and the version number should say so.

If a release is wrong, publish the fix as the next patch version and leave the bad one in
place. If it is dangerous rather than merely wrong, ask Central to mark it deprecated through
their support process; that hides it from the UI but does not delete the artifact, and anyone
who already pinned it keeps resolving it.

So the checks that matter run before the tag, not after.

## A local gotcha: the Turkish locale

On a machine with a `tr_TR` locale, the publishing plugin rejects its own configuration:

```
waitUntil must be one of the following values [uploaded, valıdated, publıshed]
```

Those are dotless `ı` characters. The plugin lowercases its allowed values without specifying a
locale, and in Turkish `I` lowercases to `ı`, so the comparison against `published` can never
match. It is a bug in the plugin, not in this project, and it does not affect CI, which runs
under a neutral locale.

If you release from a Turkish-locale machine, force the JVM's locale:

```bash
MAVEN_OPTS="-Duser.language=en -Duser.country=US" ./mvnw -B deploy -Prelease
```

## Credentials

Four GitHub Actions secrets, none of which exist outside repository settings:
`CENTRAL_TOKEN_USERNAME`, `CENTRAL_TOKEN_PASSWORD`, `GPG_PRIVATE_KEY`, `GPG_PASSPHRASE`.

To sign locally, use the gpg agent rather than putting the passphrase on a command line. The
POM deliberately configures no pinentry mode, so the agent decides how to ask.
