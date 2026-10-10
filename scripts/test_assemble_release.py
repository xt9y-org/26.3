import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile

from assemble_release import PLATFORMS, assemble


class ReleasePackagingTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for platform, filename in PLATFORMS.items():
            dest = self.root / f"mod-{platform}"
            dest.mkdir()
            with ZipFile(dest / "fabric.jar", "w") as archive:
                archive.writestr("fabric.mod.json", '{"id":"xt9y263"}')
                archive.writestr("dev/xt9y/perf/NativeBridge.class", b"common-bytecode")
                archive.writestr(f"natives/{platform}/{filename}", platform.encode("utf-8"))
        forge = self.root / "forge-base"
        forge.mkdir()
        with ZipFile(forge / "forge.jar", "w") as archive:
            archive.writestr("META-INF/mods.toml", 'modId="xt9y263"')
            archive.writestr("dev/xt9y/perf/NativeBridge.class", b"common-bytecode")

    def test_produces_two_universal_loader_jars(self):
        output = self.root / "dist"
        assemble(self.root, output, "dev-1")
        self.assertEqual(len(list(output.glob("*.jar"))), 2)
        for loader in ("fabric", "forge"):
            with ZipFile(output / f"26.3-dev-1-{loader}.jar") as archive:
                for platform, filename in PLATFORMS.items():
                    self.assertIn(f"natives/{platform}/{filename}", archive.namelist())
                self.assertIn("dev/xt9y/perf/NativeBridge.class", archive.namelist())
        self.assertIn("SHA256SUMS", (output / "SHA256SUMS.txt").read_text().replace("SUMS.txt", "SUMS"))

    def test_rejects_different_fabric_code(self):
        target = self.root / "mod-windows-x86_64" / "fabric.jar"
        with ZipFile(target, "w") as jar:
            jar.writestr("fabric.mod.json", '{"id":"xt9y263"}')
            jar.writestr("dev/xt9y/perf/NativeBridge.class", b"different")
            jar.writestr("natives/windows-x86_64/xt9y263.dll", b"native")
        with self.assertRaisesRegex(ValueError, "differ"):
            assemble(self.root, self.root / "dist", "dev-1")


if __name__ == "__main__":
    unittest.main()
