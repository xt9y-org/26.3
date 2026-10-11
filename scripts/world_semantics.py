"""Lossless *semantic* Anvil comparison for fully generated 26.3 chunks.

NBT palette entry ordering may differ while the actual block/biome positions are
identical. Decode packed palette indices and compare actual cell contents.
Only STATUS=full chunks are comparable: generated-but-unsaved neighbors and
in-progress light fields are not evidence of changed terrain.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
from collections import Counter
from pathlib import Path

from world_fingerprint import read_world


def decode_palette(container: dict, count: int, minimum_bits: int) -> list[str]:
    palette = [
        json.dumps(entry, sort_keys=True, separators=(",", ":"))
        for entry in container["palette"]
    ]
    if not palette:
        raise ValueError("Empty Minecraft paletted container")
    data = container.get("data")
    if data is None:
        if len(palette) != 1:
            raise ValueError("Missing packed data for multi-entry palette")
        return [palette[0]] * count
    packed = bytes.fromhex(data)
    if len(packed) % 8:
        raise ValueError("Packed data not divisible by 64 bits")
    values = [int.from_bytes(packed[i:i + 8], "big") for i in range(0, len(packed), 8)]
    bits = max(minimum_bits, (len(palette) - 1).bit_length())
    per_long = 64 // bits
    if len(values) < math.ceil(count / per_long):
        raise ValueError("Truncated Minecraft paletted container")
    result = []
    mask = (1 << bits) - 1
    for index in range(count):
        palette_index = (values[index // per_long] >>
                         (index % per_long * bits)) & mask
        if palette_index >= len(palette):
            raise ValueError("Out-of-range Minecraft palette index")
        result.append(palette[palette_index])
    return result


def digest(value) -> str:
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":"))
    return hashlib.sha256(encoded.encode()).hexdigest()


def completed(root: dict) -> dict | None:
    body = root.get("Level", root)
    if body.get("Status", body.get("status")) != "minecraft:full":
        return None
    if not body.get("isLightOn", True):
        return None
    sections = sorted(body.get("sections", []), key=lambda section: section.get("Y", 0))
    if not sections:
        return None
    blocks, biomes, sky, blocklight = [], [], [], []
    for section in sections:
        y = section["Y"]
        if "block_states" in section:
            blocks.append((y, digest(decode_palette(section["block_states"], 4096, 4))))
        if "biomes" in section:
            biomes.append((y, digest(decode_palette(section["biomes"], 64, 1))))
        sky.append((y, section.get("SkyLight")))
        blocklight.append((y, section.get("BlockLight")))
    return {
        "blocks": digest(blocks),
        "biomes": digest(biomes),
        "heightmaps": digest(body.get("Heightmaps", body.get("heightmaps", {}))),
        "sky_light": digest(sky),
        "block_light": digest(blocklight),
    }


def difference(reference: dict, other: dict) -> tuple[int, Counter, dict]:
    shared = reference.keys() & other.keys()
    counts = Counter()
    mismatches = {}
    for position in shared:
        changed = [field for field in reference[position]
                   if reference[position][field] != other[position][field]]
        if changed:
            mismatches[position] = changed
            counts.update(changed)
    return len(shared), counts, mismatches


def summarize(a: Path, b: Path, control: Path | None = None) -> int:
    baseline = read_world(a, completed)
    candidate = read_world(b, completed)
    shared, counts, changes = difference(baseline, candidate)
    print(f"Full/lit matching chunks: {shared}, differing: {len(changes)}")
    print(f"By field: {dict(counts)}")
    print("First differences:", sorted(changes.items())[:10])
    if shared < 32:
        print("INCONCLUSIVE: insufficient fully generated matching chunks")
        return 2
    if control is None:
        if changes:
            print("FAIL: optimized full chunk contents differ from baseline")
            return 1
        print("PASS: baseline and optimized complete chunks agree semantically")
        return 0

    reference_control = read_world(control, completed)
    control_count, variance, reference_changes = difference(
        baseline, reference_control
    )
    unexpected = changes.keys() - reference_changes.keys()
    print(f"Vanilla repeat: {control_count} shared full chunks; "
          f"{len(reference_changes)} vary naturally; by field {dict(variance)}")
    print(f"Optimized deviations outside vanilla-variation locations: "
          f"{len(unexpected)}")
    if unexpected:
        print("FAIL: differences not explained by observed vanilla-run variance")
        return 1
    if changes:
        print("INCONCLUSIVE: changes overlap vanilla variance; "
              "more fixed-workload repetitions are required")
        return 2
    print("PASS: optimized results agree with vanilla reference")
    return 0


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("optimized", type=Path)
    parser.add_argument("--control", type=Path)
    args = parser.parse_args()
    raise SystemExit(summarize(args.baseline, args.optimized, args.control))
