#!/usr/bin/env bash
#
# Checks what the archetype IT cannot: that the generated project carries the web suite, that
# the suite is wired to its own reports and message log, and that it does NOT run unless asked.
#
# The last one is the point. A generated project has to build green the minute it is generated,
# on a machine that may have no browser, so `mvn verify` must skip the web suite entirely. That
# is proved by absence: after the default build, none of the web suite's outputs exist.
#
# With --with-browser it then runs the web suite and gates it the same way the reference suites
# are gated, so "wired correctly" is a measured claim rather than a reading of the POM.
set -euo pipefail

project="${1:?usage: verify-generated-project.sh <generated-project-dir> [--with-browser]}"
mode="${2:-}"
repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$project"

fail() { echo "FAIL: $*"; exit 1; }

echo "Generated project: $project"

# Found rather than globbed at a fixed depth: the package a consumer generates into is their
# choice, so nothing here may assume how deep it is.
present() { [ -n "$(find src/test -path "*/$1" -print -quit 2>/dev/null)" ]; }
present "*WebIT.java"                  || fail "no web suite class was generated"
present "pages/ExamplePage.java"       || fail "no example page object was generated"
present "websteps/BrowserLifecycle.java" || fail "no browser lifecycle was generated"
present "features/web/example.feature" || fail "no @web feature was generated"
echo "  OK: web suite class, page object, browser lifecycle and feature are all generated."

grep -q "<id>web</id>" pom.xml || fail "no web profile in the generated POM"
grep -q "<id>web-suite</id>" pom.xml || fail "no web-suite Failsafe execution in the generated POM"
grep -q "target/failsafe-web-reports" pom.xml || fail "the web suite shares the api suite's reports directory"
grep -q "failsafe-summary-web.xml" pom.xml || fail "the web suite shares the api suite's summary file"
grep -q "web-cucumber-messages.ndjson" pom.xml || fail "the web suite shares another suite's message log"
echo "  OK: web profile and execution present, with their own reports, summary and message log."

# Proof by absence. The default build has already run by the time this script does.
for leftover in target/failsafe-web-reports target/web-cucumber-messages.ndjson; do
  [ -e "$leftover" ] && fail "$leftover exists, so the web suite ran during the default build"
done
echo "  OK: the default build produced no web output, so the web suite did not run."

if [ "$mode" != "--with-browser" ]; then
  echo "OK: generated project carries a web suite that stays out of the way."
  exit 0
fi

echo "Running the generated web suite..."
mvn -B verify -Pweb > /tmp/generated-web.log 2>&1 || {
  echo "FAIL: the generated web suite did not pass. Last lines:"; tail -40 /tmp/generated-web.log; exit 1;
}
python3 "${repo_root}/.github/scripts/execution-report.py" \
  --reports target/failsafe-web-reports \
  --messages target/web-cucumber-messages.ndjson \
  --junit-properties src/test/resources/junit-platform.properties
echo "OK: the generated web suite runs, and only when asked."
