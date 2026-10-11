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
    "dev/xt9y/perf/mixin/rendering/RenderTypeFeatureRendererAccessor.class",
    "dev/xt9y/perf/mixin/rendering/AppleFullBufferUploadMixin.class",
    "META-INF/sources/xt9y-rendering/TextGlyphConsumerMixin.java",
    "META-INF/sources/xt9y-rendering/RenderOptimizationPlugin.java",
    "META-INF/sources/xt9y-rendering/RenderTypeFeatureRendererAccessor.java",
    "META-INF/sources/xt9y-rendering/AppleFullBufferUploadMixin.java",
    "META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt",
    "META-INF/licenses/GPL-3.0-Companion.txt",
    "xt9y263-rendering.mixins.json",
    "xt9y263-hotpaths.mixins.json",
    "dev/xt9y/perf/mixin/worldgen/SeaLevelCacheMixin.class",
    "dev/xt9y/perf/mixin/hotpath/NativeHotPathPlugin.class",
    "META-INF/sources/xt9y-engine-hotpaths/worldgen/SeaLevelCacheMixin.java",
    "META-INF/sources/xt9y-engine-hotpaths/hotpath/NativeHotPathPlugin.java",
    "META-INF/licenses/LGPL-3.0-Lithium.txt",
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

        if "xt9y263-hotpaths.mixins.json" not in mod["mixins"]:
            raise ValueError("Worldgen mixin manifest registration missing")
        hotpaths = json.loads(archive.read("xt9y263-hotpaths.mixins.json"))
        # No AI pathfinding mixin is permitted: the earlier direct section
        # lookup port can change navigation semantics for aquatic mobs.
        if hotpaths.get("mixins") != ["worldgen.SeaLevelCacheMixin"]:
            raise ValueError("Unsafe worldgen/pathing mixin registration")
        if not hotpaths.get("plugin", "").endswith("NativeHotPathPlugin"):
            raise ValueError("Worldgen baseline gating missing")
        for src in (
            "META-INF/sources/xt9y-engine-hotpaths/worldgen/SeaLevelCacheMixin.java",
        ):
            content = archive.read(src).decode("utf-8")
            if "CaffeineMC" not in content or "LGPL-3.0" not in content:
                raise ValueError("Hotpath source/attribution missing")

        forbidden = (
            "dev/xt9y/perf/mixin/pathing/PathNavigationRegionMixin.class",
            "META-INF/sources/xt9y-engine-hotpaths/pathing/PathNavigationRegionMixin.java",
        )
        for path in forbidden:
            if path in archive.namelist():
                raise ValueError(f"Unsafe aquatic AI pathfinding rewrite still bundled: {path}")

        render_config = json.loads(archive.read("xt9y263-rendering.mixins.json"))
        if render_config.get("mixins", []):
            raise ValueError("Client-only rendering mixin was registered on dedicated server")
        if render_config.get("client") != [
            "TextGlyphConsumerMixin", "AppleFullBufferUploadMixin",
            "RenderTypeFeatureRendererAccessor"
        ]:
            raise ValueError("Client mixin not listed exactly once")
        if not render_config.get("plugin", "").endswith("RenderOptimizationPlugin"):
            raise ValueError("Conflict/baseline plugin is missing")

        license_text = archive.read("META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt").decode("utf-8")
        source = archive.read("META-INF/sources/xt9y-rendering/TextGlyphConsumerMixin.java").decode("utf-8")
        gpl_text = archive.read("META-INF/licenses/GPL-3.0-Companion.txt").decode("utf-8")
        if not gpl_text.startswith("GNU GENERAL PUBLIC LICENSE"):
            raise ValueError("GPLv3 companion license text missing from distributed JAR")
        if "GNU LESSER GENERAL PUBLIC LICENSE" not in license_text:
            raise ValueError("LGPL text missing from distributed JAR")
        if "ImmediatelyFast" not in source or "getVertexBuilder" not in source:
            raise ValueError("Adapted source or authorship missing from distributed JAR")

    print(f"PASS: {jar_path.name} contains isolated client rendering, source and LGPL")


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("Usage: check_render_bundle.py build/libs/26.3-...jar")
    verify(Path(sys.argv[1]))
