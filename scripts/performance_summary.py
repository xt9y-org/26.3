#!/usr/bin/env python3
"""Summarize real Minecraft server spawn-generation timing, without claiming
the warmup measurement is an overall FPS/TPS benchmark."""
import re
import sys
from pathlib import Path

PATTERN = re.compile(r"\bTime elapsed:\s*(\d+)\s*ms\b")


def parse(text: str) -> int:
    matches = [int(n) for n in PATTERN.findall(text)]
    if not matches:
        raise ValueError("No measured Minecraft spawn generation time")
    return matches[-1]


def report(baseline: int, optimized: int) -> str:
    if baseline <= 0 or optimized <= 0:
        raise ValueError("Invalid measured timing")
    return (
        "Minecraft spawn-area preparation (one run, same fixed seed)\n"
        f"Vanilla mechanics: {baseline} ms\n"
        f"Optimized bundle:  {optimized} ms\n"
        f"Ratio vanilla/optimized: {baseline / optimized:.2f}x\n"
        "This single cold-world measurement is NOT a claim about frame rate, "
        "entity throughput or repeated-run stability."
    )


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: performance_summary.py baseline.log optimized.log")
    before, after = (parse(Path(path).read_text(encoding="utf-8"))
                     for path in sys.argv[1:])
    print(report(before, after))
