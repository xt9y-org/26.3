# 26.3 — Rust Performance

Minecraft Java **26.3** dual-loader mod (Fabric and Forge) with Rust instrumentation and an experimental parallel chunk-kernel prototype.

## What currently exists

- **Fabric:** F8 toggles a Dear ImGui diagnostics panel (same renderer backend used by Personification:minecraft); open chat with T to release the mouse.
- **Forge:** F8 opens an in-game Minecraft diagnostics screen with the same Rust metrics and benchmark. Forge does not use the Fabric-specific ImGui dependency.
- Server and client tick durations are measured with Rust atomics, with UI updates limited to four per second.
- A deterministic Rust 16x16 heightfield prototype uses a bounded Rayon thread pool. In the debug menu you can set sampling quality 1–100% and worker count, then benchmark 256 chunks on a background thread.
- Native config persists in config/xt9y263.conf. If a native library is missing, the mod starts with vanilla generation unmodified.
- A single TEST GitHub Actions workflow checks Rust fmt, clippy, tests, and Fabric build.

**Scope restriction:** The live Minecraft chunk generator is NOT replaced yet, and no speedup to vanilla generation is claimed. The Rust heightfield is an isolated benchmark: its quality percentage does not change the quality or output of Minecraft's existing world generation. This matters because vanilla contains noise routing, caves, biome sampling, carvers, structures, decoration, lighting and mod hooks that must not be dropped.

## Building

Requires JDK 25, Gradle 9.7, Rust stable, Fabric Loader 0.19.5.

1. Run `gradle build` at the repository root for a local **Fabric** JAR (includes the current machine's Rust binary).
2. Run `gradle -p forge build` for a local **Forge** JAR (the native backend falls back to unavailable if not packaged).
3. GitHub Actions builds Rust for Windows x64, Linux x64, Apple Silicon and Intel macOS and assembles two universal releases: `26.3-<version>-fabric.jar` and `26.3-<version>-forge.jar`. Each universal JAR contains all four native Rust binaries, loading only the one matching the local platform.
4. Install exactly **one** loader-specific JAR. Fabric additionally needs Fabric API; Forge needs Forge 66.0.10+ for Minecraft 26.3.

Releases: https://github.com/xt9y-org/26.3/releases

The universal JAR is portable between the four supported architectures, **not** between Fabric and Forge loaders. Unsupported CPU architectures are not bundled. Native binaries are never committed to Git.

## Required parity testing before any live-world integration

A future native worldgen replacement must match vanilla byte-for-byte across multiple seeds, dimensions, chunk borders, caves, structures, biomes and decoration. Workers=1 versus workers=N must agree. No Minecraft world object may be read/written off-thread without the engine's lifecycle and ownership guarantees. Record before/after p50/p95/p99 generation latency, TPS/MSPT, allocations and memory.

Pumpkin (https://github.com/Pumpkin-MC/Pumpkin) inspires bounded parallel CPU processing and eliminating unnecessary allocations; it is not a drop-in Fabric plugin.
