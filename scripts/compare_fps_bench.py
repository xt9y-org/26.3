"""Summarize actual client frame-time logs; requires matched graphics workload."""
from pathlib import Path
import re
import sys

PATTERN = re.compile(
    r"XT9Y_FPS_BENCH frames=(\d+) avg_ms=([0-9.Ee+-]+) "
    r"p95_ms=([0-9.Ee+-]+) p99_ms=([0-9.Ee+-]+) "
    r"avg_fps=([0-9.Ee+-]+) low1pct_fps=([0-9.Ee+-]+) warmup=(\d+)"
)

def parse(text: str) -> dict:
    found = PATTERN.findall(text)
    if not found:
        raise ValueError("No actual in-world FPS benchmark completed")
    n, avg, p95, p99, fps, low, warmup = found[-1]
    if int(n) < 128:
        raise ValueError("Insufficient frames")
    return {"frames": int(n), "avg_ms": float(avg), "p95_ms": float(p95),
            "p99_ms": float(p99), "fps": float(fps),
            "low1": float(low), "warmup": int(warmup)}

def compare(baseline: dict, optimized: dict) -> str:
    lines = [
        "Real Minecraft client frame-time comparison",
        "Metric                Baseline      Optimized",
    ]
    for name in ("avg_ms", "p95_ms", "p99_ms", "fps", "low1"):
        lines.append(f"{name:<19} {baseline[name]:>10.2f} {optimized[name]:>12.2f}")
    lines.append("This A/B comparison requires identical camera, world, settings, GPU and FPS cap.")
    return "\n".join(lines)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: compare_fps_bench.py baseline.log optimized.log")
    print(compare(*(parse(Path(p).read_text(encoding="utf-8")) for p in sys.argv[1:])))
