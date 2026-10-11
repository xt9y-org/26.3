#!/usr/bin/env python3
"""Validate the distributable Fabric JAR actually includes our five engine modules.

This checks Fabric's real nested-JAR manifest, not just Gradle dependency
resolution. It intentionally fails if code is shaded or a dependency is absent.
"""
from __future__ import annotations

import io
import json
import sys
from pathlib import Path
from zipfile import ZipFile

REQUIRED = {
    "c2me": "chunk generation / scheduling",
    "scalablelux": "lighting",
    "lithium": "entity ticking and game logic",
    "immediatelyfast": "client rendering",
    "ferrite": "Rust entity query and cramming",
}

def inspect(jar_path: Path) -> set[str]:
    with ZipFile(jar_path) as root:
        assert root.testzip() is None, "Broken outer archive"
        mod = json.loads(root.read("fabric.mod.json"))
        assert mod["id"] == "xt9y263", mod["id"]
        assert "META-INF/THIRD_PARTY_NOTICES.md" in root.namelist(), "Missing credits"
        declared = [v["file"] for v in mod.get("jars", [])]
        found: set[str] = set()
        for path in declared:
            if path not in root.namelist():
                raise AssertionError(f"Missing declared nested mod: {path}")
            with ZipFile(io.BytesIO(root.read(path))) as inner:
                assert inner.testzip() is None, f"Broken nested archive: {path}"
                if "fabric.mod.json" in inner.namelist():
                    manifest = json.loads(inner.read("fabric.mod.json"))
                    found.add(manifest["id"])
        missing = REQUIRED.keys() - found
        assert not missing, f"Embedded performance modules missing: {sorted(missing)}; found: {sorted(found)}"
        print(f"{jar_path.name}: embedded {len(found)} mods; all required engines present")
        for mod_id, role in REQUIRED.items():
            print(f"  {mod_id:18s} {role}")
        return found

if __name__ == "__main__":
    jars = [Path(path) for path in sys.argv[1:]]
    if not jars:
        jars = [
            f for f in Path("build/libs").glob("*.jar")
            if not any(tag in f.name for tag in ("-sources", "-dev", "-javadoc"))
        ]
    if len(jars) != 1:
        raise SystemExit(f"Expected one production Fabric jar, got {jars}")
    inspect(jars[0])
