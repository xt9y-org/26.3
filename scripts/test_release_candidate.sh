#!/usr/bin/env bash
set -euo pipefail

# Local major-release candidate check for macOS / Linux.
# Does not publish a tag or modify an existing world.
# Requires Java 25, Gradle 9.7, Rust and Python 3.10+.
version="${XT9Y_RELEASE_VERSION:-1.0.0}"

echo "== 1/7: Python unit tests =="
python3 -m unittest discover -s scripts -p 'test_*.py'

echo "== 2/7: Rust tests =="
cargo test --manifest-path rust/Cargo.toml

echo "== 3/7: Compile the exact release version =="
gradle build "-Pversion=$version" --no-daemon

echo "== 4/7: Verify production Fabric JAR, adapted source and native library =="
python3 scripts/check_release_candidate.py --build-dir build/libs --version "$version"

echo "== 5/7: Real Minecraft server correctness oracles =="
XT9Y_TEST_BLOCKSTATES=1 bash scripts/smoke_server.sh
grep -q 'BlockState oracle passed' run/xt9y263-smoke.log
XT9Y_TEST_SHAPE_CACHE=1 bash scripts/smoke_server.sh
grep -q 'Shape cache oracle passed' run/xt9y263-smoke.log
XT9Y_TEST_CHUNK_RANDOM=1 bash scripts/smoke_server.sh
grep -q 'Random chunk tick oracle passed: 32768 exact positions' run/xt9y263-smoke.log

echo "== 6/7: Two-world lighting, worldgen and AI parity =="
bash scripts/test_lighting_parity.sh
bash scripts/test_engine_parity.sh

echo "== 7/7: Actual Minecraft dedicated-server MSPT benchmark =="
bash scripts/test_tick_benchmark.sh

echo "PASS: local server-side $version candidate checks."
echo "NOT CHECKED: real GPU output, in-world text glyphs, cross-platform Windows DLL,"
echo "             compatibility with other mods or sustained real-world FPS gains."
echo "Validate the client visually on each intended platform before publishing."
