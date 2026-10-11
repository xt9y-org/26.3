import unittest
from compare_engine_parity import parse, verify


def example(sha: str, path: int = 6144) -> str:
    return (f"XT9Y_WORLDGEN_SNAPSHOT sampled=12000 sha256={sha}\n"
            f"XT9Y_PATH_ORACLE passed={path} exact state/fluid lookups\n")


class EngineParityTests(unittest.TestCase):
    def test_real_results_match(self):
        log = example("a" * 64)
        self.assertEqual(parse(log), (12000, "a" * 64, 6144))
        self.assertIn("PASS:", verify(log, log))

    def test_different_generated_world_fails(self):
        with self.assertRaisesRegex(ValueError, "parity mismatch"):
            verify(example("a" * 64), example("b" * 64))

    def test_different_path_sample_count_fails(self):
        with self.assertRaisesRegex(ValueError, "parity mismatch"):
            verify(example("a" * 64), example("a" * 64, 5000))

    def test_missing_oracle_is_failure(self):
        with self.assertRaisesRegex(ValueError, "Missing"):
            parse("XT9Y_WORLDGEN_SNAPSHOT sampled=12000 sha256=" + "a" * 64)

    def test_too_few_samples_is_failure(self):
        with self.assertRaisesRegex(ValueError, "Insufficient"):
            parse(example("a" * 64, 100))


if __name__ == "__main__":
    unittest.main()
