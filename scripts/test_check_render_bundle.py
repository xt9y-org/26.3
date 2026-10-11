import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from check_render_bundle import FILES, verify


class RenderBundleTests(unittest.TestCase):
    def make(self, root: Path, omit: str = "", unsafe_ai_mixin: bool = False) -> Path:
        path = root / "fabric.jar"
        with ZipFile(path, "w") as jar:
            for filename in FILES:
                if filename == omit:
                    continue
                if filename == "fabric.mod.json":
                    value = json.dumps({
                        "mixins": ["xt9y263-rendering.mixins.json", "xt9y263-hotpaths.mixins.json"]
                    })
                elif filename == "xt9y263-hotpaths.mixins.json":
                    value = json.dumps({
                        "mixins": (["worldgen.SeaLevelCacheMixin", "pathing.PathNavigationRegionMixin"]
                                   if unsafe_ai_mixin else ["worldgen.SeaLevelCacheMixin"]),
                        "plugin": "example.NativeHotPathPlugin",
                    })
                elif filename == "xt9y263-rendering.mixins.json":
                    value = json.dumps({
                        "mixins": [],
                        "client": ["TextGlyphConsumerMixin", "AppleFullBufferUploadMixin", "RenderTypeFeatureRendererAccessor"],
                        "plugin": "example.RenderOptimizationPlugin",
                    })
                elif filename.endswith("ImmediatelyFast.txt"):
                    value = "GNU LESSER GENERAL PUBLIC LICENSE"
                elif filename.endswith("GPL-3.0-Companion.txt"):
                    value = "GNU GENERAL PUBLIC LICENSE"
                elif "xt9y-engine-hotpaths" in filename and filename.endswith(".java"):
                    value = "CaffeineMC LGPL-3.0 source"
                elif filename.endswith(".java"):
                    value = "ImmediatelyFast getVertexBuilder"
                else:
                    value = "bytecode"
                jar.writestr(filename, value)
        return path

    def test_valid_package(self):
        with tempfile.TemporaryDirectory() as tmp:
            verify(self.make(Path(tmp)))

    def test_missing_corresponding_source_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "META-INF/sources/xt9y-rendering/TextGlyphConsumerMixin.java")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)

    def test_missing_gpl_companion_text_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "META-INF/licenses/GPL-3.0-Companion.txt")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)

    def test_missing_worldgen_source_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "META-INF/sources/xt9y-engine-hotpaths/worldgen/SeaLevelCacheMixin.java")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)

    def test_missing_pathing_manifest_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "xt9y263-hotpaths.mixins.json")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)

    def test_path_navigation_override_is_never_registered(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), unsafe_ai_mixin=True)
            with self.assertRaisesRegex(ValueError, "Unsafe worldgen/pathing mixin registration"):
                verify(path)

    def test_compiled_navigation_override_is_forbidden(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp))
            with ZipFile(path, "a") as jar:
                jar.writestr("dev/xt9y/perf/mixin/pathing/PathNavigationRegionMixin.class", b"unsafe")
            with self.assertRaisesRegex(ValueError, "Unsafe aquatic AI pathfinding rewrite"):
                verify(path)

    def test_missing_client_mixin_metadata_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "xt9y263-rendering.mixins.json")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)


if __name__ == "__main__":
    unittest.main()
