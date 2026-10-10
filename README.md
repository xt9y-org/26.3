# 26.3 Performance

Lightweight **Minecraft Java 26.3 / Fabric only** performance telemetry.

## In-game profiler

**Right Shift** opens one compact ImGui window:

- FPS, average frame time and P95 frame time.
- TPS, average and P95 server tick time (integrated singleplayer only; no invented remote TPS).
- Real server chunk load and new-generation event counts **while the profiler is enabled**.
- Current JVM heap usage versus heap limit.

No tabs, graphs, pie charts, synthetic benchmarks, or destructive world-editing controls.

## Performance engineering

The profiler is **inactive by default**. Its Rust library is loaded only when the window is first opened; closed-window rendering and tick hooks avoid timestamp sampling, heap allocations and JNI calls. The native hot path uses bounded 240-slot atomic ring buffers. Snapshot aggregation and text formatting happen only twice per second while open.

This reduces **the mod's own monitoring overhead and startup work**. It is not a claim that unmodified Minecraft terrain generation, lighting, entity ticking, renderer, or region IO is faster. Those systems require specific profiled hot spots, behavioral equivalence tests, and in-game before/after benchmarks before modifying them safely.

The old offline chunk regeneration tool and its pending-queue processing have been removed. If you used the previous version, you can delete `config/xt9y263-pending-regen.txt`. The new build does not process that file.

## Build

Requires Java 25, Gradle 9.7, Rust stable.

```sh
cargo test --manifest-path rust/Cargo.toml
gradle build
```

CI compiles Rust on Windows x64, Linux x64, Apple Silicon and Intel macOS and releases a **single universal Fabric JAR** bundling the corresponding native libraries.

[Releases](https://github.com/xt9y-org/26.3/releases) · [CI](https://github.com/xt9y-org/26.3/actions)

For high-impact whole-game performance improvements, use verified Minecraft 26.3-compatible rendering, memory, tick and chunk-optimization mods, and profile compatibility before integrating equivalent kernels into this mod.
