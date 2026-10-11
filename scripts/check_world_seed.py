"""Assert all actual Minecraft 26.3 worlds use the same requested seed and generator.

Use the world_gen_settings.dat resource introduced by 26.3, not just level.dat
or the server.properties request. A comparator must never compare random seeds.
"""
from __future__ import annotations

import gzip
import json
import sys
from pathlib import Path

from world_fingerprint import NBT


def generator(world: Path) -> dict:
    source = world / "data" / "minecraft" / "world_gen_settings.dat"
    root = NBT(gzip.decompress(source.read_bytes())).root()
    data = root["data"]
    return {
        "seed": data["seed"],
        "generate_structures": data["generate_structures"],
        "dimensions": data["dimensions"],
        "data_version": root["DataVersion"],
    }


def validate(paths: list[Path], expected_seed: int) -> None:
    if len(paths) < 2:
        raise ValueError("Need at least two world saves")
    baseline = generator(paths[0])
    if baseline["seed"] != expected_seed:
        raise AssertionError(
            f"Baseline seed {baseline['seed']} differs from requested {expected_seed}"
        )
    for path in paths[1:]:
        actual = generator(path)
        if actual != baseline:
            raise AssertionError(
                f"{path}: generator settings differ from vanilla baseline"
            )
    print(f"Verified {len(paths)} worlds use seed {expected_seed}, "
          f"data version {baseline['data_version']} and the same generator")


if __name__ == "__main__":
    if len(sys.argv) < 4:
        raise SystemExit(
            "Usage: check_world_seed.py expected-seed vanilla-world [other-world...]"
        )
    validate([Path(p) for p in sys.argv[2:]], int(sys.argv[1]))
