# 26.3 Performance — Fabric

A **single universal Fabric JAR** with a lossless-targeting performance stack and a minimal optional Rust profiler. The actual optimizations are maintained upstream and packaged as independently loadable nested Fabric modules. No other mods need to be downloaded manually for the modules included in this bundle.

## Engine optimizations

| Game system | Included module | What it changes |
| --- | --- | --- |
| Chunk generation, scheduling and I/O | C2ME 0.4.2 alpha (26.3) | Parallel chunk management, IO, noise and scheduling |
| Block/sky lighting | ScalableLux 0.3.0 alpha (26.3) | Faster light updates and async-light infrastructure |
| Entity ticking and game logic | Lithium 0.26.2 (26.3) | Faster entity collision, AI-related queries, block ticks and other vanilla mechanics |
| Native entity hot paths | Ferrite 0.7.4 alpha (26.3) | Rust cramming and entity-query acceleration with upstream oracle checks |
| Client rendering | ImmediatelyFast 1.17.1 (26.3) | Faster immediate-mode rendering; **not a replacement for Sodium's chunk renderer** |
| Block entity rendering | Optimized Block Entities 1.1.50 (26.3) | Faster rendering for static chests, signs, beds, campfires and similar block entities while preserving animated transitions |
| Memory | FerriteCore 9.0.0 (26.3-compatible) | Less memory use and allocation pressure |
| Startup and resource loading | ModernFix-mVUS 5.27.20 (26.3) | Faster startup, resource handling, memory and initialization |

**One mod download:** all eight performance engines are already nested inside our Fabric JAR. You do **not** download those performance mods individually. Fabric Loader and Fabric API are still required, as with normal Fabric installations. These are upstream implementations embedded as separate Fabric modules, not yet code rewritten under one mod ID.

**Direct-source migration:** [ENGINE_PORTING.md](ENGINE_PORTING.md) specifies how each engine will be incorporated into our own codebase without duplicate mixins, missing licenses or quality reductions.

**Source attribution and full legal details:** [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). C2ME's proprietary OpenCL module is **not** bundled and is excluded by an automated package checker.

### Quality and simulation

No render distance, simulation distance, entity tick frequency, block lighting resolution or world generation fidelity is intentionally reduced by our distribution. Existing **Video Settings** are the user's controls for any optional quality/performance trade-offs.

The 26.3 versions of C2ME, ScalableLux and Ferrite are **alpha**. The integration is experimental until baseline/optimized world hashes, lighting comparisons, client startup and game-play stress tests pass. Upstream defaults are used. Third-party engine code is **not rewritten into our own source**; the nested JARs retain their mod identities and can be independently replaced.

Sodium and EntityCulling are not bundled because they carry separate non-permissive redistribution restrictions. Their rendering improvements are **not** represented as included features.

## In-game debugger

**Right Shift**: one minimal ImGui window with FPS, frame time/P95, local TPS/MSPT/P95, actual chunk generation counts and JVM heap usage.

Profiler is **off by default**. The Rust JNI telemetry library loads on first opening the window. Closing it disables the sampling and JNI hot path. User-facing quality controls remain in Minecraft Video Settings.

## Build

Requires Java 25, Gradle 9.7, Rust stable:

```sh
gradle build
```

Use `-Pxt9yDisableEngines=true` to build a **vanilla-mechanics baseline** for A/B parity and performance comparisons without the eight bundled engines.

CI checks Rust, builds the universal JAR, validates the embedded mod IDs and licenses, starts a real Minecraft dedicated server, and compares deterministic Anvil chunk terrain, biome and light data against the baseline. The world comparator is in `scripts/world_fingerprint.py`.

## Limitations and next engineering stages

The eight upstream engines cover substantial parts of all four requested areas, but this is **not a fully independent reimplementation** of Minecraft rendering and simulation. Major outstanding work includes chunk mesh renderer replacement (Sodium-equivalent), head-to-head FPS/1% low benchmarks in a real client, multi-seed terrain/structure/light oracle tests, and entity simulation parity on adversarial farm/modded workloads.

Never describe compilation or a startup smoke test as proof of quality equivalence or FPS uplift. Compare against vanilla and retain a rollback path.
