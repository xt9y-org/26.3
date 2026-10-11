import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch
from zipfile import ZipFile

from assemble_release import PLATFORMS
from check_release_candidate import find_jar, verify


class StableReleaseChecks(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)

    def build(self, version="1.0.0", universal=False):
        path = self.root / f"26.3-{version}-fabric.jar"
        with ZipFile(path, "w") as jar:
            jar.writestr("fabric.mod.json", json.dumps({
                "id": "xt9y263", "version": version,
                "depends": {"minecraft": "~26.3"}, "environment": "*"
            }))
            natives = list(PLATFORMS.items()) if universal else [next(iter(PLATFORMS.items()))]
            for platform, filename in natives:
                jar.writestr(f"natives/{platform}/{filename}", b"compiled-native")
        return path

    @patch("check_release_candidate.verify_mixins")
    def test_stable_major(self, _):
        verify(self.build(), "1.0.0")

    @patch("check_release_candidate.verify_mixins")
    def test_universal_platforms(self, _):
        verify(self.build(universal=True), "1.0.0", universal=True)

    @patch("check_release_candidate.verify_mixins")
    def test_wrong_version_rejected(self, _):
        with self.assertRaisesRegex(ValueError, "Wrong embedded mod version"):
            verify(self.build("1.0.0"), "2.0.0")

    @patch("check_release_candidate.verify_mixins")
    def test_missing_native_rejected(self, _):
        with self.assertRaisesRegex(ValueError, "Universal platform coverage"):
            verify(self.build(), "1.0.0", universal=True)

    def test_dev_version_does_not_filter_out_production(self):
        expected = self.build("0.2.0-dev.34")
        (self.root / "26.3-0.2.0-dev.34-sources.jar").write_bytes(b"source")
        self.assertEqual(find_jar(self.root), expected)

    @patch("check_release_candidate.verify_mixins")
    def test_invalid_version_rejected(self, _):
        with self.assertRaisesRegex(ValueError, "Invalid semantic version"):
            verify(self.build(), "../1.0.0")

    @patch("check_release_candidate.verify_mixins")
    def test_embedded_upstream_engine_rejected(self, _):
        path = self.build()
        with ZipFile(path, "a") as jar:
            jar.writestr("META-INF/jars/c2me-26.3.jar", b"not allowed")
        with self.assertRaisesRegex(ValueError, "Rejected bundled performance engine"):
            verify(path, "1.0.0")

    @patch("check_release_candidate.verify_mixins")
    def test_duplicate_entry_rejected(self, _):
        from warnings import catch_warnings, simplefilter
        path = self.build()
        with catch_warnings():
            simplefilter("ignore")
            with ZipFile(path, "a") as jar:
                jar.writestr("fabric.mod.json", "{}")
        with self.assertRaisesRegex(ValueError, "Duplicate"):
            verify(path, "1.0.0")


if __name__ == "__main__":
    unittest.main()
