"""Report server tick time from actual A/B Minecraft 26.3 workloads."""
from pathlib import Path
import re
import sys

PATTERN = re.compile(
    r"XT9Y_TPS_BENCH count=(\d+) avg_ms=([0-9.Ee+-]+) "
    r"p95_ms=([0-9.Ee+-]+) p99_ms=([0-9.Ee+-]+) "
    r"heap_mib=(\d+) warmup=(\d+)"
)

def parse(source: str) -> dict:
    matches = PATTERN.findall(source)
    if not matches:
        raise ValueError("No real Minecraft tick benchmark in log")
    count, avg, p95, p99, heap, warmup = matches[-1]
    if int(count) < 128:
        raise ValueError("Insufficient server tick samples")
    return dict(count=int(count), avg_ms=float(avg), p95_ms=float(p95),
                p99_ms=float(p99), heap_mib=int(heap), warmup=int(warmup))

def report(baseline: dict, optimized: dict) -> str:
    lines = ["### Minecraft 26.3 actual server tick A/B", "",
             "| Metric | Baseline | Optimized | Ratio (baseline/optimized) |",
             "|---|---:|---:|---:|"]
    for key, label in (("avg_ms", "Mean MSPT"), ("p95_ms", "P95 MSPT"),
                       ("p99_ms", "P99 MSPT"), ("heap_mib", "Heap MiB")):
        a, b = baseline[key], optimized[key]
        ratio = f"{a / b:.3f}x" if b else "n/a"
        lines.append(f"| {label} | {a:.3f} | {b:.3f} | {ratio} |")
    lines += ["",
              f"Samples after {baseline['warmup']}-tick warmup: "
              f"{baseline['count']} baseline, {optimized['count']} optimized.",
              "Same seed, forced 169 chunk load, one actual server run per mode.",
              "These single noisy runs do **not** establish statistical significance, "
              "actual client FPS, or universal game speedup."]
    return "\n".join(lines)

if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: compare_tick_bench.py baseline.log optimized.log")
    before, after = (parse(Path(p).read_text(encoding="utf-8"))
                     for p in sys.argv[1:])
    print(report(before, after))
