#!/usr/bin/env python3
"""Assert that the Cucumber suite actually executed work.

A green Maven build does not mean scenarios ran. Two independent checks:

  a. Surefire: tests - skipped - failures - errors, summed over the runner's reports.
     Surefire counts the JUnit Platform suite container itself, so a run where Cucumber
     matched no scenarios still reports tests="1" skipped="1". Subtracting skipped is what
     makes this measure executed work rather than mere discovery.

  b. Cucumber messages: scenarios whose every step finished PASSED. This is authoritative.
     A scenario whose steps are UNDEFINED or PENDING can still surface as a non-skipped
     Surefire testcase, and only Cucumber's own record distinguishes the two.

Note on (b): Cucumber 7.x emits testCaseFinished without a status field — it carries only
willBeRetried. Status lives on testStepFinished.testStepResult.status, so a scenario's
outcome is derived by grouping steps (and hooks) by testCaseStartedId.

With --expect-zero-scenarios the script inverts check (b) and skips (a): it is then the
negative control, asserting that a tag matching nothing really does run nothing.
"""

import json
import pathlib
import sys
import xml.etree.ElementTree as ET

REPORT_DIR = pathlib.Path("trident-runner/target")
SUREFIRE_REPORTS = REPORT_DIR / "surefire-reports"
MESSAGES = REPORT_DIR / "cucumber-messages.ndjson"


def surefire_executed_tests():
    reports = sorted(SUREFIRE_REPORTS.glob("TEST-*.xml"))
    if not reports:
        print(f"No Surefire reports found under {SUREFIRE_REPORTS}")
        return 0
    total = 0
    for report in reports:
        suite = ET.parse(report).getroot()
        counts = {k: int(suite.get(k, 0)) for k in ("tests", "skipped", "failures", "errors")}
        executed = counts["tests"] - counts["skipped"] - counts["failures"] - counts["errors"]
        print(f"  {report.name}: {counts} -> executed {executed}")
        total += executed
    return total


def passing_scenarios():
    if not MESSAGES.exists():
        print(f"No Cucumber message log found at {MESSAGES}")
        return 0
    statuses = {}
    for line in MESSAGES.read_text(encoding="utf-8").splitlines():
        if not line.strip():
            continue
        finished = json.loads(line).get("testStepFinished")
        if finished:
            case = finished["testCaseStartedId"]
            statuses.setdefault(case, []).append(finished["testStepResult"]["status"])
    return sum(1 for results in statuses.values() if results and all(s == "PASSED" for s in results))


def main():
    expect_zero = "--expect-zero-scenarios" in sys.argv[1:]

    print("Cucumber passing scenarios:")
    scenarios = passing_scenarios()
    print(f"  passing scenarios = {scenarios}")

    if expect_zero:
        if scenarios != 0:
            print(
                f"\nFAIL: negative control ran {scenarios} passing scenario(s); expected 0.\n"
                "The tag filter is not reaching the forked test JVM, so the execution gate is\n"
                "not measuring what it claims to measure."
            )
            return 1
        print("\nOK: negative control executed no scenarios, so tag forwarding is effective.")
        return 0

    print("Surefire executed tests:")
    executed = surefire_executed_tests()
    print(f"  executed tests = {executed}")

    failed = False
    if executed <= 0:
        print(f"\nFAIL: Surefire executed {executed} tests; expected more than 0.")
        failed = True
    if scenarios <= 0:
        print(f"\nFAIL: {scenarios} scenarios finished passing; expected more than 0.")
        failed = True
    if failed:
        return 1

    print(f"\nOK: {executed} executed test(s), {scenarios} passing scenario(s).")
    return 0


if __name__ == "__main__":
    sys.exit(main())
