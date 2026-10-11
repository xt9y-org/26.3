import unittest
from compare_tick_bench import parse, report

class TickBenchmarkTests(unittest.TestCase):
    def test_real_log_parse(self):
        line = ("[server/INFO] XT9Y_TPS_BENCH count=420 avg_ms=2.5 "
                "p95_ms=3.2 p99_ms=7.9 heap_mib=255 warmup=100")
        parsed = parse(line)
        self.assertEqual(parsed["count"], 420)
        self.assertIn("Mean MSPT", report(parsed, parsed))
    def test_missing_real_samples_fail(self):
        with self.assertRaises(ValueError):
            parse("No benchmark")
        with self.assertRaises(ValueError):
            parse("XT9Y_TPS_BENCH count=3 avg_ms=2 p95_ms=2 p99_ms=3 heap_mib=12 warmup=100")
