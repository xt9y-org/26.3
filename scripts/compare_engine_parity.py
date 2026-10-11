"""Fail closed if seeded worldgen fingerprints or AI block/fluid oracle differ."""
from pathlib import Path
import re
import sys

WORLD = re.compile(r"XT9Y_WORLDGEN_SNAPSHOT sampled=(\d+) sha256=([a-f0-9]{64})")
PATH = re.compile(r"XT9Y_PATH_ORACLE passed=(\d+) exact state/fluid lookups")


def parse(log: str) -> tuple[int, str, int]:
    world = WORLD.findall(log)
    path = PATH.findall(log)
    if len(world) != 1 or len(path) != 1:
        raise ValueError("Missing or duplicate real worldgen/pathfinding oracle results")
    count, digest = world[0]
    checked = int(path[0])
    if int(count) < 1000 or checked < 1000:
        raise ValueError("Insufficient chunk block state/pathfinding coverage")
    return int(count), digest, checked


def verify(baseline: str, optimized: str) -> str:
    before = parse(baseline)
    after = parse(optimized)
    if before != after:
        raise ValueError(
            f"Worldgen/AI parity mismatch: baseline={before}, optimized={after}"
        )
    return (f"PASS: equal worldgen SHA-256 over {before[0]} samples, "
            f"navigation oracle checked {before[2]} exact states/fluids")


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: compare_engine_parity.py baseline.log optimized.log")
    print(verify(*(Path(p).read_text(encoding="utf-8") for p in sys.argv[1:])))
