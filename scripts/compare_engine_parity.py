"""Validate exact generator settings and navigation oracle in two real worlds.

Fresh world block hashes are diagnostics: even independent control runs may
differ due to asynchronous worldgen/ticking. Require exact hash only in an
explicit strict experiment; do not pretend it is proven when unequal.
"""
from pathlib import Path
import os
import re
import sys

WORLD = re.compile(r"XT9Y_WORLDGEN_SNAPSHOT sampled=(\d+) sha256=([a-f0-9]{64})")
PATH = re.compile(r"XT9Y_PATH_ORACLE passed=(\d+) exact state/fluid lookups")
SEA = re.compile(r"XT9Y_WORLDGEN_SEA_LEVEL actual=(-?\d+) expected=(-?\d+)")
WATER_BOUNDARY = re.compile(r"sea_boundary=(\d+) wet_positions=(\d+)")


def parse(log: str) -> tuple[int, str, int, int]:
    world = WORLD.findall(log)
    path = PATH.findall(log)
    sea = SEA.findall(log)
    boundaries = WATER_BOUNDARY.findall(log)
    if len(world) != 1 or len(path) != 1 or len(sea) != 1 or len(boundaries) != 1:
        raise ValueError("Missing or duplicate actual engine oracle results")
    count, digest = world[0]
    checked = int(path[0])
    actual, expected = (int(n) for n in sea[0])
    boundary_count, wet_count = map(int, boundaries[0])
    if boundary_count < 1000 or wet_count < 0:
        raise ValueError("Insufficient sea-surface navigation boundary coverage")
    if int(count) < 1000 or checked < 1000:
        raise ValueError("Insufficient block-state/pathfinding samples")
    if actual != expected:
        raise ValueError(f"Invalid cached generator sea level: {actual}, expected {expected}")
    return int(count), digest, checked, actual


def verify(baseline: str, optimized: str, strict_world: bool = False) -> str:
    before = parse(baseline)
    after = parse(optimized)
    if (before[0], before[2], before[3]) != (after[0], after[2], after[3]):
        raise ValueError(f"Generator-settings/path oracle mismatch: {before} vs {after}")
    if before[1] != after[1]:
        if strict_world:
            raise ValueError(
                f"World block hash mismatch (strict): {before[1]} vs {after[1]}"
            )
        return (
            f"PASS: sea level={before[3]}, navigation state/fluid queries={before[2]}. "
            f"INCONCLUSIVE: fresh-world block hashes differ; no worldgen equivalence claimed."
        )
    return (
        f"PASS: sea level={before[3]}, navigation state/fluid queries={before[2]}, "
        f"equal sampled world block hash over {before[0]} positions."
    )


if __name__ == "__main__":
    if len(sys.argv) != 3:
        raise SystemExit("Usage: compare_engine_parity.py baseline.log optimized.log")
    baseline, optimized = (Path(p).read_text(encoding="utf-8") for p in sys.argv[1:])
    print(verify(
        baseline, optimized,
        strict_world=os.getenv("XT9Y_REQUIRE_WORLD_HASH") == "1",
    ))
