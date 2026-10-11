import unittest
from compare_fps_bench import parse, compare

class ClientFpsBenchTests(unittest.TestCase):
    def test_actual_frame_measurements(self):
        log = ("XT9Y_FPS_BENCH frames=1800 avg_ms=12.5 p95_ms=16.7 "
               "p99_ms=19.9 avg_fps=80.0 low1pct_fps=50.25 warmup=120")
        x = parse(log)
        self.assertEqual(x["frames"], 1800)
        self.assertIn("p99_ms", compare(x, x))
    def test_no_fake_numbers(self):
        with self.assertRaises(ValueError):
            parse("User did not test their actual client")
