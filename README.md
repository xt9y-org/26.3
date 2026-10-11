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

## Integrated source optimization (lossless)

**Empty data-component map compaction:** when an item's patched component map becomes empty, Minecraft now reuses a shared immutable empty map instead of retaining a per-object mutable map. Its copy-on-write flag ensures later component edits work normally. This optimization is implemented directly in our own `ComponentMapCompactionMixin.java` source, adapted from FerriteCore under MIT. The upstream duplicate is disabled automatically if FerriteCore is separately installed.

No item behavior or quality settings are changed. A real Minecraft 26.3 dedicated-server boot is included in CI to catch invalid mixin hooks. This is one limited memory optimization, not a complete rewrite of the chunk, lighting, renderer, and entity engines.

### In-house entity-tick optimization

Exact vanilla sign and furnace block entity classes no longer register a ticker
when proven idle. Editing a sign and changing furnace fuel/input force a
re-evaluation so useful ticks are restored. Mod subclass block entities
are not gated. These are ported into our own Java/mixin source from the
MIT-licensed Ferrite Minecraft 26.3 implementation. When Ferrite is also
installed, the ticker mixin set is disabled automatically to avoid duplicate
redirections. No tick-frequency or quality reduction is introduced.
