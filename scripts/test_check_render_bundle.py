import json
import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from check_render_bundle import FILES, verify


class RenderBundleTests(unittest.TestCase):
    def make(self, root: Path, omit: str = "") -> Path:
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
                        "mixins": ["worldgen.SeaLevelCacheMixin", "pathing.PathNavigationRegionMixin"],
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

    def test_missing_client_mixin_metadata_is_failure(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = self.make(Path(tmp), "xt9y263-rendering.mixins.json")
            with self.assertRaisesRegex(ValueError, "packaging incomplete"):
                verify(path)


if __name__ == "__main__":
    unittest.main()
