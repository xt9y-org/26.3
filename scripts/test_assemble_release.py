import tempfile
import unittest
from pathlib import Path
from zipfile import ZipFile
from assemble_release import PLATFORMS, assemble

class UniversalFabricTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        for platform, filename in PLATFORMS.items():
            folder = self.root / f"mod-{platform}"
            folder.mkdir()
            with ZipFile(folder / "fabric.jar", "w") as jar:
                jar.writestr("fabric.mod.json", '{"id":"xt9y263"}')
                jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
                jar.writestr(f"natives/{platform}/{filename}", platform.encode())

    def test_universal_fabric_jar(self):
        output = self.root / "dist"
        assemble(self.root, output, "dev-1")
        self.assertEqual(len(list(output.glob("*.jar"))), 1)
        with ZipFile(output / "26.3-dev-1-fabric.jar") as jar:
            for platform, filename in PLATFORMS.items():
                self.assertIn(f"natives/{platform}/{filename}", jar.namelist())
        self.assertEqual(len((output / "SHA256SUMS.txt").read_text().splitlines()), 1)

    def test_normalizes_windows_crlf_resources(self):
        path = self.root / "mod-windows-x86_64" / "fabric.jar"
        with ZipFile(path, "w") as jar:
            jar.writestr("fabric.mod.json", b'{\r\n  "id": "xt9y263"\r\n}\r\n')
            jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
            jar.writestr("natives/windows-x86_64/xt9y263.dll", b"win")
        # Other platforms must have semantically identical text resources.
        for platform, filename in PLATFORMS.items():
            if platform == "windows-x86_64":
                continue
            location = self.root / f"mod-{platform}" / "fabric.jar"
            with ZipFile(location, "w") as jar:
                jar.writestr("fabric.mod.json", b'{\n  "id": "xt9y263"\n}\n')
                jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
                jar.writestr(f"natives/{platform}/{filename}", platform.encode())
        assemble(self.root, self.root / "dist", "dev-2")

    def test_normalizes_windows_license_crlf(self):
        path = self.root / "mod-windows-x86_64" / "fabric.jar"
        with ZipFile(path, "w") as jar:
            jar.writestr("fabric.mod.json", '{"id":"xt9y263"}')
            jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
            jar.writestr("META-INF/licenses/MIT-FerriteCore.txt", b"line one\r\nline two\r\n")
            jar.writestr("natives/windows-x86_64/xt9y263.dll", b"win")
        for platform, filename in PLATFORMS.items():
            if platform == "windows-x86_64":
                continue
            location = self.root / f"mod-{platform}" / "fabric.jar"
            with ZipFile(location, "w") as jar:
                jar.writestr("fabric.mod.json", '{"id":"xt9y263"}')
                jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
                jar.writestr("META-INF/licenses/MIT-FerriteCore.txt", b"line one\nline two\n")
                jar.writestr(f"natives/{platform}/{filename}", platform.encode())
        assemble(self.root, self.root / "dist", "dev-3")
        with ZipFile(self.root / "dist/26.3-dev-3-fabric.jar") as jar:
            self.assertEqual(jar.read("META-INF/licenses/MIT-FerriteCore.txt"),
                             b"line one\nline two\n")

    def test_normalizes_embedded_lgpl_source_crlf(self):
        source = "META-INF/sources/xt9y-chunk-random/ChunkRandomSource.java"
        for platform, filename in PLATFORMS.items():
            jar_path = self.root / f"mod-{platform}" / "fabric.jar"
            content = b"class Source {\r\n int x;\r\n}\r\n" if platform.startswith("windows") else b"class Source {\n int x;\n}\n"
            with ZipFile(jar_path, "w") as jar:
                jar.writestr("fabric.mod.json", '{"id":"xt9y263"}')
                jar.writestr("dev/xt9y/perf/NativeBridge.class", b"code")
                jar.writestr(source, content)
                jar.writestr(f"natives/{platform}/{filename}", platform.encode())
        assemble(self.root, self.root / "dist", "dev-src")
        with ZipFile(self.root / "dist/26.3-dev-src-fabric.jar") as jar:
            self.assertEqual(jar.read(source), b"class Source {\n int x;\n}\n")

    def test_rejects_code_mismatch(self):
        path = self.root / "mod-windows-x86_64/fabric.jar"
        with ZipFile(path, "w") as jar:
            jar.writestr("fabric.mod.json", '{"id":"different"}')
            jar.writestr("natives/windows-x86_64/xt9y263.dll", b"native")
        with self.assertRaisesRegex(ValueError, "differ"):
            assemble(self.root, self.root / "dist", "dev-1")

if __name__ == "__main__":
    unittest.main()
