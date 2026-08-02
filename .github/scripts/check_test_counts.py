#!/usr/bin/env python3
"""Fail the build when a module that has tests ran none of them.

Gradle reports BUILD SUCCESSFUL for a test task that executed nothing. That is not a
hypothetical here: `core/ui`'s accessibility suite existed for months while running
zero tests, because only `:app` declared a `testInstrumentationRunner`, and three
core modules later crashed on a runner they did not ship. Both looked like a green
build from the outside.

So a green task is not the signal. The signal is: every module with test *sources*
produced a result file with a non-zero count. A module whose suite silently stops
executing fails here instead of passing quietly.

Usage: check_test_counts.py {unit|instrumented}
"""
from __future__ import annotations

import sys
import xml.etree.ElementTree as ET
from pathlib import Path

KINDS = {
    # kind: (source dir under a module, globs for that task's result XMLs)
    #
    # Unit results land in two places because the project mixes module types: Android
    # libraries write `testDebugUnitTest`, while the pure-JVM ones (core:common,
    # core:domain, via moneyflow.jvm.library) write plain `test`. Globbing only the
    # first is how those two modules can look like they have no tests at all.
    "unit": (
        "src/test",
        ("build/test-results/testDebugUnitTest/*.xml", "build/test-results/test/*.xml"),
    ),
    "instrumented": (
        "src/androidTest",
        ("build/outputs/androidTest-results/connected/**/*.xml",),
    ),
}

ROOT = Path(__file__).resolve().parents[2]


def modules() -> list[Path]:
    """Gradle modules, as the two-level `group/name` layout this project uses."""
    found = []
    for group in ("app", "core", "feature"):
        base = ROOT / group
        if not base.is_dir():
            continue
        if (base / "build.gradle.kts").is_file():
            found.append(base)
        for child in sorted(base.iterdir()):
            if (child / "build.gradle.kts").is_file():
                found.append(child)
    return found


def has_sources(module: Path, src_dir: str) -> bool:
    path = module / src_dir
    return path.is_dir() and any(path.rglob("*.kt"))


def counts(module: Path, patterns: tuple[str, ...]) -> tuple[int, int, int]:
    tests = failures = errors = 0
    for xml in (x for pattern in patterns for x in module.glob(pattern)):
        root = ET.parse(xml).getroot()
        tests += int(root.get("tests") or 0)
        failures += int(root.get("failures") or 0)
        errors += int(root.get("errors") or 0)
    return tests, failures, errors


def main() -> int:
    if len(sys.argv) != 2 or sys.argv[1] not in KINDS:
        print(f"usage: {Path(__file__).name} {{{'|'.join(KINDS)}}}", file=sys.stderr)
        return 2

    kind = sys.argv[1]
    src_dir, patterns = KINDS[kind]

    silent: list[str] = []
    failed: list[str] = []
    total = 0

    print(f"{kind} test counts")
    print("-" * 52)
    for module in modules():
        name = module.relative_to(ROOT).as_posix().replace("/", ":")
        if not has_sources(module, src_dir):
            continue
        tests, failures, errors = counts(module, patterns)
        total += tests
        flag = ""
        if tests == 0:
            silent.append(name)
            flag = "  <-- has sources, ran nothing"
        if failures or errors:
            failed.append(name)
            flag = f"  <-- {failures} failed, {errors} errored"
        print(f"{name:<32} {tests:>4}{flag}")
    print("-" * 52)
    print(f"total: {total}")

    if silent:
        print(
            "\nERROR: these modules have "
            f"{src_dir} sources but produced no test results:\n  "
            + "\n  ".join(silent)
            + "\n\nA suite that runs nothing reports the same green build as one that "
            "passes.\nCheck the runner wiring before trusting this result.",
            file=sys.stderr,
        )
    if failed:
        print(f"\nERROR: failing tests in: {', '.join(failed)}", file=sys.stderr)
    if total == 0:
        print("\nERROR: no tests ran at all.", file=sys.stderr)
        return 1

    return 1 if (silent or failed) else 0


if __name__ == "__main__":
    sys.exit(main())
