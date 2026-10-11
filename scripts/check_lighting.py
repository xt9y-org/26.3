"""Compare real Minecraft block- and sky-light fields in matched server runs."""
from pathlib import Path
import re
import sys

PATTERN = re.compile(r"XT9Y_LIGHT_ORACLE lit=([a-f0-9]{64}) dark=([a-f0-9]{64}) positions=(\d+)")

def parse(text: str) -> tuple[str, str, int]:
    found = PATTERN.findall(text)
    if not found:
        raise ValueError("No completed Minecraft lighting oracle in server log")
    lit, dark, count = found[-1]
    if lit == dark or int(count) != 8125:
        raise ValueError("Lighting oracle fixture did not change")
    return lit, dark, int(count)

def compare(before: str, after: str) -> None:
    vanilla = parse(before)
    optimized = parse(after)
    if vanilla != optimized:
        raise AssertionError(
            f"Real Minecraft light propagation changed: vanilla={vanilla}, "
            f"optimized={optimized}"
        )
    print(f"PASS: 2 × {vanilla[2]} identical block-light/skylight samples "
          "after adding/removing glowstone and an occlusion wall")

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: check_lighting.py baseline.log optimized.log")
    compare(*(Path(p).read_text(encoding="utf-8") for p in sys.argv[1:]))
