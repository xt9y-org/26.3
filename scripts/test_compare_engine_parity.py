import unittest
from compare_engine_parity import parse, verify


def example(sha: str, path: int = 6144, level: int = 63) -> str:
    return (f"XT9Y_WORLDGEN_SNAPSHOT sampled=5000 sha256={sha}\n"
            f"XT9Y_PATH_ORACLE passed={path} exact state/fluid lookups\n"
            f"XT9Y_WORLDGEN_SEA_LEVEL actual={level} expected={level}\n")


class EngineParityTests(unittest.TestCase):
    def test_actual_oracles_match(self):
        log = example("a" * 64)
        self.assertEqual(parse(log), (5000, "a" * 64, 6144, 63))
        self.assertIn("equal sampled world block hash", verify(log, log))

    def test_world_hash_drift_is_not_mislabeled_as_parity(self):
        output = verify(example("a" * 64), example("b" * 64))
        self.assertIn("INCONCLUSIVE", output)
        self.assertIn("no worldgen equivalence claimed", output)

    def test_strict_world_hash_mismatch_fails(self):
        with self.assertRaisesRegex(ValueError, "World block hash mismatch"):
            verify(example("a" * 64), example("b" * 64), strict_world=True)

    def test_path_sample_mismatch_fails(self):
        with self.assertRaisesRegex(ValueError, "Generator-settings/path oracle mismatch"):
            verify(example("a" * 64), example("a" * 64, 5000))

    def test_sea_level_mismatch_fails(self):
        with self.assertRaisesRegex(ValueError, "Generator-settings/path oracle mismatch"):
            verify(example("a" * 64), example("a" * 64, level=62))

    def test_no_sea_level_oracle_fails(self):
        with self.assertRaisesRegex(ValueError, "Missing"):
            parse("XT9Y_WORLDGEN_SNAPSHOT sampled=5000 sha256=" + "a" * 64)

    def test_insufficient_samples_fail(self):
        with self.assertRaisesRegex(ValueError, "Insufficient"):
            parse(example("a" * 64, 100))


if __name__ == "__main__":
    unittest.main()
