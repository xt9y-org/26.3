import unittest
from performance_summary import parse, report


class SpawnTimingTests(unittest.TestCase):
    def test_extracts_last_elapsed_time(self):
        text = "[main/INFO] Time elapsed: 921 ms\n" \
               "[Server thread/INFO] (Minecraft) Time elapsed: 4291 ms\n"
        self.assertEqual(parse(text), 4291)

    def test_ratio_preserves_actual_units(self):
        result = report(2000, 1000)
        self.assertIn("2.00x", result)
        self.assertIn("NOT a claim", result)

    def test_refuses_fabricated_zero(self):
        with self.assertRaises(ValueError):
            report(120, 0)
        with self.assertRaises(ValueError):
            parse("No timing")
