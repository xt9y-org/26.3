import unittest
from world_semantics import completed, decode_palette, difference


def states(palette, indices):
    bits = max(4, (len(palette) - 1).bit_length())
    mask = (1 << bits) - 1
    width = 64 // bits
    packed = []
    for offset in range(0, len(indices), width):
        word = 0
        for bit, index in enumerate(indices[offset:offset + width]):
            word |= (index & mask) << (bit * bits)
        packed.append(word.to_bytes(8, "big"))
    return {"palette": palette, "data": b"".join(packed).hex()}


def chunk(palette, indices):
    return {
        "Status": "minecraft:full",
        "isLightOn": 1,
        "sections": [{
            "Y": 0,
            "block_states": states(palette, indices),
            "biomes": {"palette": ["minecraft:plains"]},
            "SkyLight": "ff",
            "BlockLight": "00",
        }],
        "Heightmaps": {"WORLD_SURFACE": "0100"},
    }


class SemanticParityTests(unittest.TestCase):
    def test_same_voxels_different_palette_indices(self):
        a = chunk(["stone", "dirt"], [0, 1] * 2048)
        b = chunk(["dirt", "stone"], [1, 0] * 2048)
        self.assertEqual(completed(a), completed(b))

    def test_real_voxel_difference_is_detected(self):
        a = chunk(["stone", "dirt"], [0, 1] * 2048)
        b = chunk(["stone", "dirt"], [0, 1] * 2048)
        b["sections"][0]["block_states"] = states(
            ["stone", "dirt"], [0, 0] + [0, 1] * 2047
        )
        self.assertNotEqual(completed(a)["blocks"], completed(b)["blocks"])

    def test_light_difference_is_detected(self):
        a = chunk(["stone"], [0] * 4096)
        b = chunk(["stone"], [0] * 4096)
        b["sections"][0]["SkyLight"] = "fd"
        self.assertNotEqual(completed(a)["sky_light"], completed(b)["sky_light"])
        self.assertEqual(completed(a)["blocks"], completed(b)["blocks"])

    def test_incomplete_chunks_not_counted(self):
        a = chunk(["stone"], [0] * 4096)
        a["Status"] = "minecraft:initialize_light"
        self.assertIsNone(completed(a))
        a["Status"] = "minecraft:full"
        a["isLightOn"] = 0
        self.assertIsNone(completed(a))

    def test_variance_is_reported_by_category(self):
        a = completed(chunk(["stone"], [0] * 4096))
        b = completed(chunk(["dirt"], [0] * 4096))
        shared, counts, mismatches = difference({(0, 0): a}, {(0, 0): b})
        self.assertEqual(shared, 1)
        self.assertEqual(counts["blocks"], 1)
        self.assertEqual(mismatches[(0, 0)], ["blocks"])


if __name__ == "__main__":
    unittest.main()
