#!/usr/bin/env python3
"""Fail-closed validation of a compiled or universal Minecraft Fabric release.

Never accept a source-only JAR, incorrect mod version, extra/missing platform
native, duplicate ZIP entry, absent mixin, or missing redistributed source.
"""
from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from zipfile import ZipFile

if __package__:
    from .assemble_release import PLATFORMS
    from .check_render_bundle import verify as verify_mixins
else:
    from assemble_release import PLATFORMS
    from check_render_bundle import verify as verify_mixins

SEMVER = re.compile(r"(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)\.(?:0|[1-9][0-9]*)(?:-dev\.[0-9]+)?$")
NON_PRODUCTION_SUFFIX = re.compile(r"-(?:sources|dev|javadoc|all)\.jar$")


def find_jar(build_dir: Path) -> Path:
    candidates = sorted(
        p for p in build_dir.glob("*.jar")
        if not NON_PRODUCTION_SUFFIX.search(p.name)
    )
    if len(candidates) != 1:
        raise ValueError(f"Expected exactly one production Fabric JAR: {candidates}")
    return candidates[0]


def verify(jar: Path, version: str, universal: bool = False) -> None:
    if not SEMVER.fullmatch(version):
        raise ValueError(f"Invalid semantic version: {version}")
    if not jar.is_file():
        raise ValueError(f"Missing compiled JAR: {jar}")

    verify_mixins(jar)

    with ZipFile(jar) as z:
        names = z.namelist()
        if len(names) != len(set(names)):
            raise ValueError("Duplicate ZIP entry in release archive")
        # One-file install must not silently bundle the previously rejected
        # eight-engine experimental stack. The ImGui GUI library is allowed.
        upstream_engines = (
            "c2me", "lithium", "scalablelux", "immediatelyfast",
            "modernfix", "ferritecore", "ferrite", "optimizedblockentities",
        )
        for name in names:
            if name.lower().endswith(".jar") and any(
                engine in name.rsplit("/", 1)[-1].lower()
                for engine in upstream_engines
            ):
                raise ValueError(f"Rejected bundled performance engine: {name}")
        if z.testzip() is not None:
            raise ValueError("Corrupt entry in release archive")
        manifest = json.loads(z.read("fabric.mod.json"))
        if manifest.get("id") != "xt9y263":
            raise ValueError("Wrong mod id in release")
        if manifest.get("version") != version:
            raise ValueError(
                f"Wrong embedded mod version: {manifest.get('version')!r}, expected {version}"
            )
        if manifest.get("depends", {}).get("minecraft") != "~26.3":
            raise ValueError("Not a Minecraft 26.3 Fabric distribution")
        if manifest.get("environment") != "*":
            raise ValueError("Expected universal client/server Fabric environment")

        paths = {name for name in names if name.startswith("natives/") and not name.endswith("/")}
        expected = {
            f"natives/{platform}/{native}"
            for platform, native in PLATFORMS.items()
        }
        if universal:
            if paths != expected:
                raise ValueError(f"Universal platform coverage mismatch: {paths ^ expected}")
        elif len(paths) != 1 or next(iter(paths)) not in expected:
            raise ValueError(f"Wrong single-platform native payload: {paths}")
        for entry in paths:
            if not z.read(entry):
                raise ValueError(f"Empty native payload: {entry}")
    print(f"PASS: {jar.name}: version={version}, natives={len(paths)}, native source and licenses")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--build-dir", type=Path, required=True)
    parser.add_argument("--version", required=True)
    parser.add_argument("--universal", action="store_true")
    args = parser.parse_args()
    verify(find_jar(args.build_dir), args.version, universal=args.universal)
