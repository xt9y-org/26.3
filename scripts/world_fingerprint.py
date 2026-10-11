#!/usr/bin/env python3
"""Compare deterministic worldgen blocks/biomes/heightmaps/lighting from Anvil saves.

Read-only. Ignores volatile tick timestamps and compares overlapping generated
Overworld chunks. Never treats a bare server-start benchmark as parity proof.
"""
from __future__ import annotations

import argparse
import gzip
import hashlib
import io
import json
import re
import struct
import zlib
from pathlib import Path


class NBT:
    def __init__(self, source: bytes):
        if len(source) > 16_000_000:
            raise ValueError("NBT chunk payload too large")
        self.data = memoryview(source)
        self.cursor = 0

    def take(self, length: int) -> bytes:
        if length < 0 or length > 16_000_000 or self.cursor + length > len(self.data):
            raise ValueError("NBT field exceeds chunk data")
        start = self.cursor
        self.cursor += length
        return self.data[start:self.cursor].tobytes()

    def integer(self, fmt: str) -> int | float:
        return struct.unpack(">" + fmt, self.take(struct.calcsize(">" + fmt)))[0]

    def string(self) -> str:
        length = int(self.integer("H"))
        return self.take(length).decode("utf-8")

    def value(self, kind: int, depth: int = 0):
        if depth > 64:
            raise ValueError("NBT nesting too deep")
        primitives = {1: "b", 2: "h", 3: "i", 4: "q", 5: "f", 6: "d"}
        if kind in primitives:
            return self.integer(primitives[kind])
        if kind == 7:
            return self.take(int(self.integer("i"))).hex()
        if kind == 8:
            return self.string()
        if kind == 9:
            element = int(self.integer("B"))
            count = int(self.integer("i"))
            if count < 0 or count > 2_000_000:
                raise ValueError("Invalid NBT list length")
            return [self.value(element, depth + 1) for _ in range(count)]
        if kind == 10:
            value = {}
            while True:
                next_kind = int(self.integer("B"))
                if next_kind == 0:
                    return value
                name = self.string()
                value[name] = self.value(next_kind, depth + 1)
        if kind in (11, 12):
            count = int(self.integer("i"))
            unit = 4 if kind == 11 else 8
            if count < 0 or count > 2_000_000:
                raise ValueError("Invalid NBT array length")
            # raw big-endian byte representation preserves bits and avoids boxing.
            return self.take(count * unit).hex()
        raise ValueError(f"Unsupported NBT tag {kind}")

    def root(self):
        if self.integer("B") != 10:
            raise ValueError("Root must be a compound")
        self.string()  # root name
        root = self.value(10)
        if self.cursor != len(self.data):
            raise ValueError("Trailing NBT bytes")
        return root


def essential_chunk(root: dict) -> dict | None:
    if not isinstance(root, dict):
        return None
    body = root.get("Level", root)
    sections = body.get("sections", body.get("Sections", []))
    if not isinstance(sections, list):
        return None
    # Terrain blocks and biome palette, including structures and decoration.
    keep = ("Y", "block_states", "biomes", "SkyLight", "BlockLight",
            "Palette", "BlockStates", "Biomes")
    data = [{name: section[name] for name in keep if name in section}
            for section in sections if isinstance(section, dict)]
    if not data:
        return None
    data.sort(key=lambda section: section.get("Y", 0))
    return {
        "sections": data,
        "heightmaps": body.get("Heightmaps", body.get("heightmaps", {})),
        "light_valid": body.get("isLightOn", None),
    }


def fingerprint(root: dict) -> str | None:
    data = essential_chunk(root)
    if data is None:
        return None
    stable = json.dumps(data, sort_keys=True, separators=(",", ":"), allow_nan=False)
    return hashlib.sha256(stable.encode()).hexdigest()


def read_region(path: Path) -> dict[tuple[int, int], str]:
    match = re.fullmatch(r"r\.(-?\d+)\.(-?\d+)\.mca", path.name)
    if not match:
        return {}
    rx, rz = (int(v) for v in match.groups())
    source = path.read_bytes()
    if len(source) < 8192 or len(source) % 4096:
        raise ValueError(f"Invalid Anvil region size: {path}")
    results = {}
    for slot in range(1024):
        position = int.from_bytes(source[slot * 4:slot * 4 + 3], "big")
        if not position:
            continue
        start = position * 4096
        if start + 5 > len(source):
            raise ValueError(f"Out-of-bounds chunk in {path} slot {slot}")
        length = struct.unpack_from(">I", source, start)[0]
        if not 1 <= length <= 16_000_000 or start + 4 + length > len(source):
            raise ValueError(f"Invalid chunk length in {path} slot {slot}")
        codec = source[start + 4]
        if codec & 128:
            raise ValueError("External .mcc chunk requires separate parity support")
        payload = source[start + 5:start + 4 + length]
        if codec == 1:
            raw = gzip.decompress(payload)
        elif codec == 2:
            raw = zlib.decompress(payload)
        elif codec == 3:
            raw = payload
        else:
            raise ValueError(f"Unsupported Anvil compression codec {codec}")
        digest = fingerprint(NBT(raw).root())
        if digest is None:
            continue
        x = rx * 32 + (slot % 32)
        z = rz * 32 + (slot // 32)
        results[(x, z)] = digest
    return results


def read_world(world: Path) -> dict[tuple[int, int], str]:
    # Minecraft 26.3 stores even the Overworld in its namespaced dimension
    # directory; older versions use the root-level region/ directory.
    candidates = [
        world / "dimensions" / "minecraft" / "overworld" / "region",
        world / "region",
    ]
    for directory in candidates:
        if not directory.is_dir():
            continue
        result = {}
        for path in sorted(directory.glob("r.*.*.mca")):
            result.update(read_region(path))
        if result:
            return result
    raise ValueError(
        f"No generated Overworld chunk sections in any of {candidates}"
    )


def compare(a: dict, b: dict, minimum: int = 16) -> None:
    shared = a.keys() & b.keys()
    if len(shared) < minimum:
        raise AssertionError(f"Only {len(shared)} common chunks, need >= {minimum}")
    mismatches = [pos for pos in sorted(shared) if a[pos] != b[pos]]
    if mismatches:
        raise AssertionError(
            f"{len(mismatches)} / {len(shared)} chunks differ in blocks, "
            f"biomes, heightmaps or lighting; first 8: {mismatches[:8]}"
        )
    print(f"PASS: {len(shared)} common chunks are block/biome/heightmap/light identical")
    print(f"Baseline: {len(a)} chunks; optimized: {len(b)} chunks")
    print("Note: other entity simulation timing and asynchronous races are not covered")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("baseline", type=Path)
    parser.add_argument("optimized", type=Path)
    parser.add_argument("--minimum", type=int, default=16)
    args = parser.parse_args()
    compare(read_world(args.baseline), read_world(args.optimized), args.minimum)
