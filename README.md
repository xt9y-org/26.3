# 26.3 — Fabric Rust Performance

**Minecraft Java 26.3 / Fabric only.** There is no Forge build, metadata, entrypoint, or supported Forge runtime.

## Debug dashboard

Press **Right Shift** to toggle the Dear ImGui panel. Open chat with **T** when you need to release the mouse.

Five tabs:

- **Overview** — actual sampled frame intervals, FPS, measured CPU renderer span, server and client tick times.
- **FPS / Frames** — FPS/frame-time history and measured CPU-renderer versus *unattributed* frame time. GPU and per-pass costs are not yet measured.
- **TPS / Ticks** — real tick duration and recent effective TPS. The pie chart is tick execution versus a 50 ms tick budget, **not** an invented entity/block/network breakdown.
- **Chunks** — live server/client loaded and unloaded chunk counts, plus Minecraft's generated flag received from Fabric's server chunk-load callback. No fake Rust chunk-generation benchmark remains.
- **Tools** — destructive Overworld chunk regeneration, radius 0–2 around the player, integrated singleplayer only. Requires explicit confirmation.

## How chunk regeneration works

This is an **offline** Rust-backed modification of real world data, not the synthetic benchmark.

1. Back up the world yourself before testing.
2. In singleplayer in the **Overworld**, open **Tools**, select radius and confirm. This writes a pending regeneration request into the Fabric configuration directory; the live world is not altered.
3. **Exit Minecraft completely**, then restart. On startup Rust checks `session.lock` is exclusively available. It refuses to edit an open or locked save.
4. For selected chunk slots, Rust backs up affected Anvil files and removes location/timestamp references in the `region`, `entities`, and `poi` files. Existing neighboring chunk entries remain unchanged. Oversized external `.mcc` chunks are rejected rather than processed unsafely.
5. Vanilla Minecraft generates the absent chunks (including lighting) the next time they are requested. Backups reside in `xt9y263-backups/` under the world folder.

**WARNING:** This permanently replaces anything built or stored in the selected chunks, including containers, entities, and POI. The backup is required for recovery. Editing chunk persistence is experimental, and the operation currently does not extend to the Nether, End, multiplayer or mod-specific sidecar databases. Failed operations keep the queue and attempt a backup-based rollback. Do not use on an important world without a complete backup.

## Rust engine

The Rust cdylib processes actual frame/tick samples in lock-free bounded 240-sample histories, tracks server and client chunk lifecycle counts, computes averages, and carries out queued *offline* region-file operations. JNI transports measurements and commands. The Fabric JVM entrypoint and ImGui renderer remain Java code because Minecraft Java Edition runs inside the JVM.

No change to the live vanilla noise router or light engine is made yet. Asynchronous generator replacement requires exact parity across seeds, biomes, cave/structure boundaries, mod-added features, and full lighting, plus measurements against baseline. The current code does **not** claim an FPS/TPS/world-generation speedup.

## Build and release

Requires Java 25, Gradle 9.7, and Rust stable.

```sh
cargo test --manifest-path rust/Cargo.toml
gradle build
```

The local JAR contains the native Rust library for the build machine. GitHub Actions builds on Linux x64, Windows x64, macOS ARM64, macOS x64, then assembles **one universal Fabric JAR** containing all four Rust libraries.

Releases: https://github.com/xt9y-org/26.3/releases

## Future profiling-driven optimizations

Benchmark and instrument real chunk scheduling, generation status stages, structure placement, lighting propagation, region I/O/compression, meshing and rendering synchronization. Then move only isolated CPU kernels into Rust when it preserves Minecraft's semantics and outperforms the Java implementation after JNI overhead.

All world-generation changes must be opt-in until parity, mod compatibility and long-duration stress tests pass.
