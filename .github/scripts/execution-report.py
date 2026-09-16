#!/usr/bin/env python3
"""Assert that the Cucumber suite actually executed work.

A green Maven build is not evidence that anything ran. Three ways it can lie, all of which
were reproduced against this repository before this script was written:

  * Surefire counts the JUnit Platform suite container itself, so a run where Cucumber
    matched no scenarios still reports tests="1" skipped="1" and failIfNoTests stays quiet.
  * Surefire 3.5.3 reported tests="0" with no <testcase> for a suite that demonstrably ran.
  * A dry run reports every step PASSED without invoking a single step body.

The checks below are therefore independent, and each one is the other's backstop.

On dry-run detection: a dry run's message stream is byte-for-byte ordinary — same envelopes,
every step PASSED, testRunFinished success=true. This was verified, not assumed. There is no
marker to look for, so dry-run is detected from configuration instead. The limitation that
follows is that a dry run forced with -Dcucumber.execution.dry-run=true on the Maven command
line is invisible here, because this script runs in a different process; the committed
properties file is the vector that matters in CI.

With --expect-zero-scenarios the script becomes the negative control: it asserts that a tag
matching nothing really did run nothing. That requires a complete message log showing zero
scenarios started — a missing log is a failure, not a pass, because absence of evidence is
not evidence that the filter worked.
"""

import json
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

MODULE = pathlib.Path("trident-runner")
SUREFIRE_REPORTS = MODULE / "target" / "surefire-reports"
MESSAGES = MODULE / "target" / "cucumber-messages.ndjson"
JUNIT_PROPERTIES = MODULE / "src" / "test" / "resources" / "junit-platform.properties"

DRY_RUN_SETTING = re.compile(r"^\s*cucumber\.execution\.dry-run\s*[=:]\s*true\s*$", re.IGNORECASE)


def dry_run_configured():
    """True if junit-platform.properties enables dry-run. Commented lines do not count."""
    if not JUNIT_PROPERTIES.exists():
        return False
    for line in JUNIT_PROPERTIES.read_text(encoding="utf-8").splitlines():
        if line.lstrip().startswith(("#", "!")):
            continue
        if DRY_RUN_SETTING.match(line):
            return True
    return False


def read_messages():
    """Reduce the NDJSON stream to the few relations the checks need."""
    stream = {
        "run_finished": False,
        "pickle_steps": {},  # pickle id -> set of step ids it declares
        "test_cases": {},  # test case id -> (pickle id, {test step id -> pickle step id})
        "started": {},  # test case started id -> test case id
        "finished_cases": set(),  # test case started ids with a testCaseFinished
        "step_results": {},  # test case started id -> {test step id: status}
    }
    if not MESSAGES.exists():
        return stream, False

    for line in MESSAGES.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        envelope = json.loads(line)

        if "testRunFinished" in envelope:
            stream["run_finished"] = True

        if "pickle" in envelope:
            pickle = envelope["pickle"]
            stream["pickle_steps"][pickle["id"]] = {s["id"] for s in pickle.get("steps", [])}

        if "testCase" in envelope:
            case = envelope["testCase"]
            steps = {s["id"]: s.get("pickleStepId") for s in case.get("testSteps", [])}
            stream["test_cases"][case["id"]] = (case["pickleId"], steps)

        if "testCaseStarted" in envelope:
            started = envelope["testCaseStarted"]
            stream["started"][started["id"]] = started["testCaseId"]

        if "testCaseFinished" in envelope:
            stream["finished_cases"].add(envelope["testCaseFinished"]["testCaseStartedId"])

        if "testStepFinished" in envelope:
            step = envelope["testStepFinished"]
            results = stream["step_results"].setdefault(step["testCaseStartedId"], {})
            results[step["testStepId"]] = step["testStepResult"]["status"]

    return stream, True


def passing_scenarios(stream):
    """Scenarios that started, finished, ran every declared step, and passed throughout.

    Every clause matters. Counting PASSED step results alone lets an orphaned
    testStepFinished — from a truncated stream — register as a passing scenario.
    """
    passing = 0
    for started_id, case_id in stream["started"].items():
        if started_id not in stream["finished_cases"]:
            continue
        case = stream["test_cases"].get(case_id)
        if case is None:
            continue
        pickle_id, steps = case
        declared = stream["pickle_steps"].get(pickle_id, set())
        results = stream["step_results"].get(started_id, {})

        ran = {pickle_step for step_id, pickle_step in steps.items() if step_id in results}
        if not declared or not declared.issubset(ran):
            continue
        if not results or any(status != "PASSED" for status in results.values()):
            continue
        passing += 1
    return passing


def surefire_executed_tests():
    reports = sorted(SUREFIRE_REPORTS.glob("TEST-*.xml"))
    if not reports:
        print(f"  no Surefire reports under {SUREFIRE_REPORTS}")
        return 0
    total = 0
    for report in reports:
        suite = ET.parse(report).getroot()
        counts = {k: int(suite.get(k, 0)) for k in ("tests", "skipped", "failures", "errors")}
        executed = counts["tests"] - counts["skipped"] - counts["failures"] - counts["errors"]
        print(f"  {report.name}: {counts} -> executed {executed}")
        total += executed
    return total


def main():
    expect_zero = "--expect-zero-scenarios" in sys.argv[1:]
    stream, log_present = read_messages()
    scenarios = passing_scenarios(stream)
    failures = []

    print("Cucumber message log:")
    print(f"  file present          = {log_present} ({MESSAGES})")
    print(f"  testRunFinished       = {stream['run_finished']}")
    print(f"  scenarios started     = {len(stream['started'])}")
    print(f"  scenarios finished    = {len(stream['finished_cases'])}")
    print(f"  scenarios passing     = {scenarios}")
    print(f"  dry-run configured    = {dry_run_configured()} ({JUNIT_PROPERTIES})")

    if dry_run_configured():
        failures.append(
            f"{JUNIT_PROPERTIES} enables cucumber.execution.dry-run. A dry run reports every\n"
            "step PASSED without invoking any step body, so it can never satisfy this gate."
        )

    if not log_present:
        failures.append(f"No Cucumber message log at {MESSAGES}; the suite did not report.")
    elif not stream["run_finished"]:
        failures.append(
            "The message log has no testRunFinished envelope. The stream is truncated, so its\n"
            "counts are not trustworthy."
        )

    if expect_zero:
        if not failures and len(stream["started"]) != 0:
            failures.append(
                f"Negative control started {len(stream['started'])} scenario(s); expected 0.\n"
                "The tag filter is not reaching the forked test JVM, so the execution gate is\n"
                "not measuring what it claims to measure."
            )
        if failures:
            print("\nFAIL:\n" + "\n\n".join(failures))
            return 1
        print("\nOK: negative control completed a run and started no scenarios, so tag")
        print("forwarding into the forked JVM is effective.")
        return 0

    print("Surefire reports:")
    executed = surefire_executed_tests()
    print(f"  executed tests        = {executed}")

    if executed <= 0:
        failures.append(f"Surefire executed {executed} tests; expected more than 0.")
    if scenarios <= 0:
        failures.append(
            f"{scenarios} scenarios started, finished, ran every declared step and passed;\n"
            "expected more than 0."
        )

    if failures:
        print("\nFAIL:\n" + "\n\n".join(failures))
        return 1

    print(f"\nOK: {executed} executed test(s), {scenarios} fully-executed passing scenario(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
