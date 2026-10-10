#!/usr/bin/env python3
"""Turn platform-specific Fabric jars and one Forge base jar into two universal jars.

The Rust JNI library is identical across loaders but has per-OS/arch builds.
The script refuses to publish if Fabric class/resource payloads differ between
runners or if any expected native library is missing.
"""
from __future__ import annotations

import argparse
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED, is_zipfile

PLATFORMS = {
    "linux-x86_64": "libxt9y263.so",
    "windows-x86_64": "xt9y263.dll",
    "macos-aarch64": "libxt9y263.dylib",
    "macos-x86_64": "libxt9y263.dylib",
}


def only_jar(directory: Path) -> Path:
    jars = list(directory.glob("*.jar"))
    if len(jars) != 1:
        raise ValueError(f"Expected exactly one jar in {directory}, found {jars}")
    if not is_zipfile(jars[0]):
        raise ValueError(f"Invalid jar: {jars[0]}")
    return jars[0]


def payload(path: Path) -> tuple[dict[str, bytes], dict[str, bytes]]:
    with ZipFile(path) as jar:
        names = jar.namelist()
        if len(names) != len(set(names)):
            raise ValueError(f"Duplicate archive entries in {path}")
        files = {name: jar.read(name) for name in names if not name.endswith("/")}
    natives = {name: data for name, data in files.items() if name.startswith("natives/")}
    common = {name: data for name, data in files.items() if not name.startswith("natives/")}
    return common, natives


def assemble(directory: Path, output: Path, version: str) -> None:
    common_base: dict[str, bytes] | None = None
    native_files: dict[str, bytes] = {}
    for platform, filename in PLATFORMS.items():
        jar = only_jar(directory / f"mod-{platform}")
        common, natives = payload(jar)
        if common_base is None:
            common_base = common
        elif common.keys() != common_base.keys() or any(
            common[key] != common_base[key] for key in common_base
        ):
            raise ValueError(f"Fabric archives differ beyond their native libraries: {platform}")
        expected = f"natives/{platform}/{filename}"
        if set(natives) != {expected} or not natives[expected]:
            raise ValueError(
                f"Unexpected/missing natives for {platform}: {sorted(natives)}"
            )
        native_files.update(natives)

    forge, forge_native = payload(only_jar(directory / "forge-base"))
    if forge_native:
        raise ValueError("Forge base jar unexpectedly includes native resources")
    if "META-INF/mods.toml" not in forge or "fabric.mod.json" in forge:
        raise ValueError("Forge metadata is missing or contains Fabric metadata")
    assert common_base is not None
    if "fabric.mod.json" not in common_base or "META-INF/mods.toml" in common_base:
        raise ValueError("Fabric metadata is missing or contains Forge metadata")

    output.mkdir(parents=True, exist_ok=True)
    checksum_lines = []
    for loader, base in [("fabric", common_base), ("forge", forge)]:
        destination = output / f"26.3-{version}-{loader}.jar"
        with ZipFile(destination, "w", compression=ZIP_DEFLATED, compresslevel=6) as jar:
            for name, data in sorted({**base, **native_files}.items()):
                jar.writestr(name, data)
        with ZipFile(destination) as jar:
            missing = set(native_files) - set(jar.namelist())
            if missing or jar.testzip() is not None:
                raise ValueError(f"Invalid universal jar: {destination}")
        checksum_lines.append(
            f"{sha256(destination.read_bytes()).hexdigest()}  {destination.name}"
        )
        print(f"Built {destination} ({destination.stat().st_size} bytes)")
    (output / "SHA256SUMS.txt").write_text(
        "\n".join(checksum_lines) + "\n", encoding="utf-8"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    assemble(args.input_root, args.output, args.version)
