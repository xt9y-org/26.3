#!/usr/bin/env python3
"""Report actual Minecraft spawn-area preparation, with repeated vanilla control.

These are cold-start observations, not engine-wide FPS/TPS or conclusive speedups.
"""
from __future__ import annotations

import re
import sys
from pathlib import Path

PATTERN = re.compile(r"\bTime elapsed:\s*(\d+)\s*ms\b")


def parse(log: str) -> int:
    matches = [int(n) for n in PATTERN.findall(log)]
    if not matches:
        raise ValueError("No Minecraft spawn-area time elapsed in log")
    return matches[-1]


def report(baseline: int, optimized: int, control: int | None = None) -> str:
    if baseline <= 0 or optimized <= 0 or (control is not None and control <= 0):
        raise ValueError("Missing or nonpositive Minecraft timing")
    lines = [
        "Measured Minecraft spawn-area preparation (same world seed)",
        f"Vanilla baseline A: {baseline} ms",
    ]
    if control is not None:
        lines += [
            f"Vanilla baseline B: {control} ms",
            f"Vanilla run-to-run variation: {abs(baseline - control)} ms",
        ]
        baseline_mean = (baseline + control) / 2
    else:
        baseline_mean = baseline
    lines += [
        f"Optimized run: {optimized} ms",
        f"Baseline mean / optimized: {baseline_mean / optimized:.2f}x",
        "One or two vanilla cold runs are insufficient to prove repeatable speedup. "
        "These timings exclude client rendering FPS and sustained server TPS.",
    ]
    return "\n".join(lines)


if __name__ == "__main__":
    if len(sys.argv) not in (3, 4):
        raise SystemExit(
            "Usage: performance_summary.py vanilla-A.log optimized.log [vanilla-B.log]"
        )
    before, after = (parse(Path(p).read_text(encoding="utf-8")) for p in sys.argv[1:3])
    control = parse(Path(sys.argv[3]).read_text(encoding="utf-8")) if len(sys.argv) == 4 else None
    print(report(before, after, control))
