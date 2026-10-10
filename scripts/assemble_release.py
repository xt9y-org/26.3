"""Merge per-platform Fabric JARs into one universal Fabric/Rust JAR."""
import argparse
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED

PLATFORMS = {
    "linux-x86_64": "libxt9y263.so",
    "windows-x86_64": "xt9y263.dll",
    "macos-aarch64": "libxt9y263.dylib",
    "macos-x86_64": "libxt9y263.dylib",
}


def assemble(directory: Path, output: Path, version: str) -> None:
    common = None
    native_files = {}
    for platform, filename in PLATFORMS.items():
        paths = list((directory / f"mod-{platform}").glob("*.jar"))
        if len(paths) != 1:
            raise ValueError(f"Expected exactly one Fabric jar for {platform}: {paths}")
        with ZipFile(paths[0]) as jar:
            if jar.testzip() is not None:
                raise ValueError(f"Corrupt archive: {paths[0]}")
            names = jar.namelist()
            if len(names) != len(set(names)):
                raise ValueError(f"Duplicate entries: {paths[0]}")
            files = {name: jar.read(name) for name in names if not name.endswith("/")}
        expected = f"natives/{platform}/{filename}"
        natives = {name: data for name, data in files.items() if name.startswith("natives/")}
        if set(natives) != {expected} or not natives[expected]:
            raise ValueError(f"Missing/unexpected native for {platform}: {list(natives)}")
        # Git autocrlf may rewrite JSON resources on Windows. Normalize text
        # before comparing and embedding; never normalize class or native bytes.
        code = {
            name: data.replace(b"\r\n", b"\n") if name.endswith(".json") else data
            for name, data in files.items() if not name.startswith("natives/")
        }
        if common is None:
            common = code
        elif code != common:
            raise ValueError(f"Fabric code/resources differ between platforms: {platform}")
        native_files.update(natives)
    if not common or "fabric.mod.json" not in common:
        raise ValueError("Missing Fabric mod manifest")
    if "META-INF/mods.toml" in common:
        raise ValueError("Forge metadata found in Fabric distribution")
    output.mkdir(parents=True, exist_ok=True)
    output_jar = output / f"26.3-{version}-fabric.jar"
    with ZipFile(output_jar, "w", ZIP_DEFLATED, compresslevel=6) as jar:
        for name, data in sorted({**common, **native_files}.items()):
            jar.writestr(name, data)
    with ZipFile(output_jar) as jar:
        if jar.testzip() is not None:
            raise ValueError("Generated archive is corrupt")
    (output / "SHA256SUMS.txt").write_text(
        f"{sha256(output_jar.read_bytes()).hexdigest()}  {output_jar.name}\n", encoding="utf-8"
    )


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--input-root", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--version", required=True)
    args = parser.parse_args()
    assemble(args.input_root, args.output, args.version)
