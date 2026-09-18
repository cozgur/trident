#!/usr/bin/env python3
"""Assert that the generated Allure report covers every suite.

A report that generates successfully is not a report that contains anything. Allure writes an
index.html whether or not any result reached it, so the check that matters is the scenario
count and the spread across suites — the same numbers the execution gates assert, arrived at
from the other end.
"""

import argparse
import json
import pathlib
import sys


def leaves(node):
    if "children" in node:
        for child in node["children"]:
            yield from leaves(child)
    else:
        yield node


def main():
    parser = argparse.ArgumentParser(description="Check the Allure report's contents.")
    parser.add_argument("--report", type=pathlib.Path, required=True,
                        help="the generated report directory")
    parser.add_argument("--expect-scenarios", type=int, required=True)
    parser.add_argument("--expect-suites", type=int, required=True,
                        help="how many feature groupings the report should show")
    args = parser.parse_args()

    suites_file = args.report / "data" / "suites.json"
    if not suites_file.is_file():
        print(f"FAIL: no suites.json under {args.report}; the report has no results in it.")
        return 1

    data = json.loads(suites_file.read_text(encoding="utf-8"))
    groups = data.get("children", [])
    tests = list(leaves(data))

    print(f"Report at {args.report}:")
    for group in groups:
        names = list(leaves(group))
        print(f"  {group.get('name')}: {len(names)} scenario(s)")
        for t in names:
            print(f"    {t.get('status', '?'):8} {t.get('name', '?')}")

    failures = []
    if len(tests) != args.expect_scenarios:
        failures.append(f"expected {args.expect_scenarios} scenarios in the report, found {len(tests)}")
    if len(groups) != args.expect_suites:
        failures.append(f"expected {args.expect_suites} feature groupings, found {len(groups)}")
    not_passed = [t.get("name") for t in tests if t.get("status") != "passed"]
    if not_passed:
        failures.append("these scenarios are not passed: " + ", ".join(not_passed))

    if failures:
        print("\nFAIL:\n  " + "\n  ".join(failures))
        return 1

    print(f"\nOK: {len(tests)} scenarios across {len(groups)} feature groupings, all passed.")
    return 0


if __name__ == "__main__":
    sys.exit(main())
