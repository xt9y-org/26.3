import unittest
from check_lighting import parse, compare

class LightParityTests(unittest.TestCase):
    def test_repeated_exact_fields(self):
        s = f"XT9Y_LIGHT_ORACLE lit={'a'*64} dark={'b'*64} positions=8125"
        self.assertEqual(parse(s)[2], 8125)
        compare(s, s)
    def test_changed_light_fails(self):
        a = f"XT9Y_LIGHT_ORACLE lit={'a'*64} dark={'b'*64} positions=8125"
        b = f"XT9Y_LIGHT_ORACLE lit={'c'*64} dark={'b'*64} positions=8125"
        with self.assertRaises(AssertionError):
            compare(a, b)
    def test_empty_data_fails(self):
        with self.assertRaises(ValueError):
            parse("No light data")
