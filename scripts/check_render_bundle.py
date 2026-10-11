#!/usr/bin/env python3
"""Check the actual compiled Fabric archive contains our legal source port.

Compilation alone is insufficient: Java classes, Mixin configuration and
corresponding LGPL source must survive Loom remapping and JAR assembly.
"""
from __future__ import annotations

import json
import sys
from pathlib import Path
from zipfile import ZipFile

FILES = (
    "dev/xt9y/perf/mixin/rendering/TextGlyphConsumerMixin.class",
    "dev/xt9y/perf/mixin/rendering/RenderOptimizationPlugin.class",
    "dev/xt9y/perf/mixin/rendering/AppleFullBufferUploadMixin.class",
    "META-INF/sources/xt9y-rendering/TextGlyphConsumerMixin.java",
    "META-INF/sources/xt9y-rendering/RenderOptimizationPlugin.java",
    "META-INF/sources/xt9y-rendering/AppleFullBufferUploadMixin.java",
    "META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt",
    "xt9y263-rendering.mixins.json",
    "fabric.mod.json",
)


def verify(jar_path: Path) -> None:
    with ZipFile(jar_path) as archive:
        corrupted = archive.testzip()
        if corrupted is not None:
            raise ValueError(f"Corrupted archive entry: {corrupted}")
        missing = [name for name in FILES if name not in archive.namelist()]
        if missing:
            raise ValueError(f"Rendering optimization packaging incomplete: {missing}")

        mod = json.loads(archive.read("fabric.mod.json"))
        if "xt9y263-rendering.mixins.json" not in mod["mixins"]:
            raise ValueError("Rendering mixin configuration missing from Fabric manifest")

        render_config = json.loads(archive.read("xt9y263-rendering.mixins.json"))
        if render_config.get("mixins", []):
            raise ValueError("Client-only rendering mixin was registered on dedicated server")
        if render_config.get("client") != [
            "TextGlyphConsumerMixin", "AppleFullBufferUploadMixin"
        ]:
            raise ValueError("Client mixin not listed exactly once")
        if not render_config.get("plugin", "").endswith("RenderOptimizationPlugin"):
            raise ValueError("Conflict/baseline plugin is missing")

        license_text = archive.read("META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt").decode("utf-8")
        source = archive.read("META-INF/sources/xt9y-rendering/TextGlyphConsumerMixin.java").decode("utf-8")
        if "GNU LESSER GENERAL PUBLIC LICENSE" not in license_text:
            raise ValueError("LGPL text missing from distributed JAR")
        if "ImmediatelyFast" not in source or "getVertexBuilder" not in source:
            raise ValueError("Adapted source or authorship missing from distributed JAR")

    print(f"PASS: {jar_path.name} contains isolated client rendering, source and LGPL")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: check_render_bundle.py build/libs/26.3-...jar")
    verify(Path(sys.argv[1]))
