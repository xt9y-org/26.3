import struct
import tempfile
import unittest
import zlib
from pathlib import Path

from world_fingerprint import NBT, compare, essential_chunk, fingerprint, read_region, read_world


class ChunkParityTests(unittest.TestCase):
    @staticmethod
    def block(name: str = "minecraft:stone"):
        return {
            "sections": [{
                "Y": 0,
                "block_states": {"palette": [{"Name": name}]},
                "biomes": {"palette": ["minecraft:plains"]},
                "SkyLight": "ff",
                "BlockLight": "00",
            }],
            "Heightmaps": {"WORLD_SURFACE": "00000000"},
        }

    def test_exact_match_ignores_unstable_metadata(self):
        first = self.block()
        first["LastUpdate"] = 123
        first["InhabitedTime"] = 456
        second = self.block()
        second["LastUpdate"] = 987
        self.assertEqual(fingerprint(first), fingerprint(second))
        self.assertEqual(len(fingerprint(first)), 64)
        compare({(0, 0): fingerprint(first)}, {(0, 0): fingerprint(second)}, 1)

    def test_changes_in_blocks_lighting_or_biomes_fail(self):
        baseline = self.block()
        for change in ["block", "light", "biome", "heightmap"]:
            updated = self.block()
            if change == "block":
                updated["sections"][0]["block_states"]["palette"][0]["Name"] = "minecraft:dirt"
            elif change == "light":
                updated["sections"][0]["SkyLight"] = "01"
            elif change == "biome":
                updated["sections"][0]["biomes"]["palette"][0] = "minecraft:forest"
            else:
                updated["Heightmaps"]["WORLD_SURFACE"] = "01000000"
            self.assertNotEqual(fingerprint(baseline), fingerprint(updated))
            with self.assertRaises(AssertionError):
                compare({(0, 0): fingerprint(baseline)}, {(0, 0): fingerprint(updated)}, 1)

    def test_region_reads_real_zlib_encoded_nbt(self):
        def named(tag: int, key: str, value: bytes) -> bytes:
            name = key.encode()
            return bytes([tag]) + struct.pack(">H", len(name)) + name + value

        # root (compound) -> sections (list of compound) -> Y (byte)
        section = named(1, "Y", b"\x00") + b"\x00"
        root = (
            b"\x0a\x00\x00" +
            named(9, "sections", b"\x0a" + struct.pack(">i", 1) + section) +
            b"\x00"
        )
        self.assertEqual(NBT(root).root()["sections"][0]["Y"], 0)
        self.assertIsNotNone(essential_chunk(NBT(root).root()))
        compressed = zlib.compress(root)
        record = struct.pack(">I", len(compressed) + 1) + b"\x02" + compressed
        region = bytearray(3 * 4096)
        region[:4] = b"\x00\x00\x02\x01"
        region[8192:8192 + len(record)] = record
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "r.-1.0.mca"
            path.write_bytes(region)
            read = read_region(path)
            self.assertIn((-32, 0), read)
            self.assertEqual(len(read[(-32, 0)]), 64)
            namespaced = Path(directory) / "dimensions/minecraft/overworld/region"
            namespaced.mkdir(parents=True)
            (namespaced / path.name).write_bytes(path.read_bytes())
            result = read_world(Path(directory))
            self.assertEqual(result, read)


if __name__ == "__main__":
    unittest.main()
