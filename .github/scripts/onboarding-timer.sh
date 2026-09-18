#!/usr/bin/env bash
#
# Measures the claim the README makes: seconds from `archetype:generate` to a suite that
# provably ran, resolving every Trident artifact from Maven Central.
#
# Four details are load-bearing.
#
#   * A warm-up project is generated and run first, and thrown away. Without it the number
#     is a function of whether the CI runner happened to restore a Maven cache, which is not
#     a property of Trident. The warm-up fills ~/.m2 with everything that is not Trident.
#   * The timed run then resolves every Trident artifact from Central, because the local
#     repository is emptied of dev.ozgurcetintas immediately before it. So the measurement
#     is the README's: a developer who already has Maven and Java, adopting Trident today.
#   * The archetype version comes from Central's metadata, not from this checkout. What is
#     timed is the released onboarding path, which may lag main by a commit or a month.
#   * "Green" means the scenario executed, checked with the same gate the suites use. Maven
#     exits 0 for a suite that matched nothing, and a number derived from that would be a
#     measurement of the empty set.
#
# Writes {"seconds": N} only when all of that held. Any failure leaves no file, and facts.py
# publishes null rather than a number it cannot stand behind.
set -euo pipefail

out="${1:?usage: onboarding-timer.sh <output-json>}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
mkdir -p "$(dirname "$out")"
out="$(cd "$(dirname "$out")" && pwd)/$(basename "$out")"

# The generated project pins its own test locale, but Maven's plugins run in the shell's.
# On a Turkish-locale machine that is not cosmetic: see ADR 0016.
export MAVEN_OPTS="${MAVEN_OPTS:-} -Duser.language=en -Duser.country=US"

group_path="https://repo1.maven.org/maven2/dev/ozgurcetintas/trident"
version="$(curl -fsSL "${group_path}/trident-bom/maven-metadata.xml" \
  | sed -n 's:.*<release>\(.*\)</release>.*:\1:p')"
if [ -z "$version" ]; then
  echo "No released version on Central; nothing to time."
  exit 1
fi
echo "Timing onboarding against the released archetype ${version}."

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

generate_and_run() {
  local dir="$1"
  (
    cd "$work"
    mvn -B -q archetype:generate \
      -DarchetypeGroupId=dev.ozgurcetintas.trident \
      -DarchetypeArtifactId=trident-archetype \
      -DarchetypeVersion="$version" \
      -DgroupId=io.acme.qa \
      -DartifactId="$dir" \
      -Dprefix=Probe \
      -DinteractiveMode=false
    cd "$dir"
    mvn -B -Psmoke verify
  )
}

echo "--- warm-up run (discarded) ---"
generate_and_run warmup > "${work}/warmup.log" 2>&1 || {
  echo "Warm-up failed; last lines:"; tail -30 "${work}/warmup.log"; exit 1;
}

echo "--- timed run ---"
rm -rf "${HOME}/.m2/repository/dev/ozgurcetintas"
start=$(date +%s)
generate_and_run onboarding-probe
seconds=$(( $(date +%s) - start ))

python3 "${repo_root}/.github/scripts/execution-report.py" \
  --reports "${work}/onboarding-probe/target/surefire-reports" \
  --messages "${work}/onboarding-probe/target/cucumber-messages.ndjson" \
  --junit-properties "${work}/onboarding-probe/src/test/resources/junit-platform.properties"

printf '{\n  "seconds": %s,\n  "archetypeVersion": "%s"\n}\n' "$seconds" "$version" > "$out"
echo "Onboarding took ${seconds}s; wrote ${out}."
