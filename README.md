# 26.3 — Rust Performance

Minecraft Java **26.3** Fabric mod with Rust instrumentation and an experimental parallel chunk-kernel prototype.

## What currently exists

- F8 toggles a clean ImGui diagnostics panel (same ImGui dependency and renderer hook as Personification:minecraft). Open chat with T to release your mouse while inspecting the window.
- Server and client tick durations are measured with Rust atomics, with UI updates limited to four per second.
- A deterministic Rust 16x16 heightfield prototype uses a bounded Rayon thread pool. In the debug menu you can set sampling quality 1–100% and worker count, then benchmark 256 chunks on a background thread.
- Native config persists in config/xt9y263.conf. If a native library is missing, the mod starts with vanilla generation unmodified.
- A single TEST GitHub Actions workflow checks Rust fmt, clippy, tests, and Fabric build.

**Scope restriction:** The live Minecraft chunk generator is NOT replaced yet, and no speedup to vanilla generation is claimed. The Rust heightfield is an isolated benchmark: its quality percentage does not change the quality or output of Minecraft's existing world generation. This matters because vanilla contains noise routing, caves, biome sampling, carvers, structures, decoration, lighting and mod hooks that must not be dropped.

## Building

Requires JDK 25, Gradle 9.6, Rust stable, Fabric Loader 0.19.5.

1. cd rust && cargo test && cargo build --release
2. For a single-architecture packaged jar, copy your compiled native library to src/main/resources/natives/<os>-<arch>/ with its original system name. macos-aarch64 uses libxt9y263.dylib; linux-x86_64 uses libxt9y263.so; windows-x86_64 uses xt9y263.dll.
3. gradle build
4. Install the jar on Fabric 26.3 alongside Fabric API. The ImGui dependency is included during the Gradle jar build.

During development, alternatively add rust/target/release to java.library.path and the mod will load it directly.

No native binaries are committed. Native builds must be produced for each supported platform before distributing a universal jar.

## Required parity testing before any live-world integration

A future native worldgen replacement must match vanilla byte-for-byte across multiple seeds, dimensions, chunk borders, caves, structures, biomes and decoration. Workers=1 versus workers=N must agree. No Minecraft world object may be read/written off-thread without the engine's lifecycle and ownership guarantees. Record before/after p50/p95/p99 generation latency, TPS/MSPT, allocations and memory.

Pumpkin (https://github.com/Pumpkin-MC/Pumpkin) inspires bounded parallel CPU processing and eliminating unnecessary allocations; it is not a drop-in Fabric plugin.
