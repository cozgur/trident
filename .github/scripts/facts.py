#!/usr/bin/env python3
"""Assemble the numbers this project publishes about itself.

The portfolio at ozgurcetintas.dev fills each project's fact line from a facts.json that the
project's own CI writes, so the sentence on the site cannot quietly drift from the build.
That only works if nothing here is typed in. Every value below is read from an artifact of
the run that produced it, from the repository, or from Maven Central itself.

A value that cannot be derived is written as null. The portfolio drops null clauses from the
sentence rather than printing a placeholder, so an underivable number costs a clause and
never becomes a claim.
"""

import argparse
import json
import pathlib
import re
import sys
import urllib.error
import urllib.request
from datetime import datetime, timezone

CENTRAL = "https://repo1.maven.org/maven2/dev/ozgurcetintas/trident"
ADR_FILE = re.compile(r"^\d{4}-.*\.md$")
RELEASE = re.compile(r"<release>([^<]+)</release>")
ARTIFACT_DIR = re.compile(r'href="([a-zA-Z0-9._-]+)/"')


def fetch(url):
    """Return the body, or None. Central being unreachable is not a build failure."""
    try:
        with urllib.request.urlopen(url, timeout=20) as response:
            return response.read().decode("utf-8")
    except (urllib.error.URLError, OSError, ValueError) as error:
        print(f"  {url}: unreachable ({error})")
        return None


def released_version():
    """The BOM's release version on Central. Every release publishes it, so it anchors the rest."""
    body = fetch(f"{CENTRAL}/trident-bom/maven-metadata.xml")
    if body is None:
        return None
    match = RELEASE.search(body)
    return match.group(1) if match else None


def published_modules(version):
    """Artifacts on Central whose current release is this version.

    Read from the repository rather than from the reactor: the question is what a consumer
    can resolve today, and a module this checkout builds is not published until Central says
    so. Counting the group directory alone would also count an artifact dropped in a later
    release.
    """
    if version is None:
        return None
    listing = fetch(f"{CENTRAL}/")
    if listing is None:
        return None
    artifacts = [name for name in ARTIFACT_DIR.findall(listing) if name != ".."]
    published = []
    for artifact in artifacts:
        body = fetch(f"{CENTRAL}/{artifact}/maven-metadata.xml")
        match = RELEASE.search(body) if body else None
        if match and match.group(1) == version:
            published.append(artifact)
    print(f"  on Central at {version}: {', '.join(published)}")
    return len(published) or None


def count_adrs(root):
    directory = root / "docs" / "adr"
    if not directory.is_dir():
        return None
    return len([p for p in directory.iterdir() if ADR_FILE.match(p.name)]) or None


def read_json(path):
    if not path.is_file():
        print(f"  {path}: absent")
        return None
    try:
        return json.loads(path.read_text(encoding="utf-8"))
    except json.JSONDecodeError as error:
        print(f"  {path}: not readable ({error})")
        return None


def scenario_total(evidence):
    """Scenarios that the execution gate was willing to pass, across both suites.

    Summed rather than taken from one file: the smoke suite and the api suite run in separate
    JVMs under separate plugins and each writes its own message log. A missing suite makes the
    total unknown rather than smaller — a half-counted total is worse than no number.
    """
    counts = []
    for name in ("smoke.json", "api.json"):
        report = read_json(evidence / name)
        if report is None or report.get("scenarios") is None:
            print(f"  {name}: no scenario count, so the total is not derivable")
            return None
        counts.append(report["scenarios"])
    return sum(counts)


def main():
    parser = argparse.ArgumentParser(description="Write the facts this project publishes.")
    parser.add_argument("--evidence", type=pathlib.Path, default=pathlib.Path("evidence"),
                        help="directory holding the JSON written by this run's gates")
    parser.add_argument("--out", type=pathlib.Path, default=pathlib.Path("site/facts.json"))
    parser.add_argument("--repo-root", type=pathlib.Path, default=pathlib.Path("."))
    args = parser.parse_args()

    print("Maven Central:")
    version = released_version()
    modules = published_modules(version)

    print("This run's evidence:")
    scenarios = scenario_total(args.evidence)
    onboarding = read_json(args.evidence / "onboarding.json")

    facts = {
        "name": "Trident",
        "releasedVersion": version,
        "publishedModules": modules,
        "scenarios": scenarios,
        "adrs": count_adrs(args.repo_root),
        "archetypeToGreenSeconds": onboarding.get("seconds") if onboarding else None,
        "generatedAt": datetime.now(timezone.utc).isoformat(timespec="milliseconds").replace("+00:00", "Z"),
    }

    args.out.parent.mkdir(parents=True, exist_ok=True)
    args.out.write_text(json.dumps(facts, indent=2) + "\n", encoding="utf-8")
    print(f"\nWrote {args.out}:")
    print(json.dumps(facts, indent=2))

    # Publishing a document with nothing in it would be worse than not publishing: the
    # portfolio would silently keep its committed sentence and nobody would know why.
    derived = [k for k, v in facts.items() if v is not None and k not in ("name", "generatedAt")]
    if not derived:
        print("\nFAIL: not one value could be derived.")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
