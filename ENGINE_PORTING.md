# Converging the one-JAR engine stack into xt9y-owned implementations

## What users install today

The distributable artifact is **one** Fabric JAR. It carries eight separate
upstream performance modules as *nested* JARs (plus the ImGui client adapter and
our native Rust profiler). Fabric Loader discovers the embedded module IDs.
There are no separate downloads for those eight performance engines.

Minecraft itself, Fabric Loader and currently **Fabric API** remain prerequisites.
Bundling Fabric API is a distinct compatibility project and is not implied by
the optimization stack.

This is one download, **not yet a single in-house engine implementation**.
Upstream algorithms and ownership must not be misrepresented.

## Source integration policy

Every converted algorithm must become a genuine source file in this repository,
not a renamed outer JAR. Conversion is replacement, not duplication: remove
the corresponding upstream nested module/mixin *only after* equivalent
optimization coverage and tests exist. A transition may bundle the remaining
upstream modules until the last one is replaced.

For each port:
1. Identify the **exact upstream release and original source commit**, classes,
   data structures, thread-safety contract, load-order requirements and license.
2. Audit its licenses **per directory and per file**. Preserve notices,
   attribution and any source redistribution/relinking rights. Do not copy
   C2ME's nonredistributable OpenCL module or Sodium's non-permissive code.
3. Implement the engine-independent primitives directly under
   \`dev.xt9y.perf.engine.{chunks,lighting,rendering,entities,memory}\`, using
   dedicated Fabric mixins only where a real 26.3 target exists.
4. Add identity/oracle tests **before enabling** that optimization. Test
   complete chunk block/biome/structure/light contents, cross-chunk boundaries,
   entity tick sequences, bounding boxes and rendering pixels as applicable.
5. Benchmark cold and warm server worlds, entity farms, crowded scenes,
   frame times (including 1% lows), TPS/MSPT, heap and GC under identical
   seeds/hardware. Account for JNI and scheduling overhead.
6. Remove the superseded nested module, update the one-JAR bundle gate and
   repeat all CI, dedicated-server and client startup tests.
7. Leave all lossless optimizations enabled by default. Any shortcut that
   changes simulation or rendering belongs **only** in an explicit user opt-in
   setting, with accurate quality trade-off language.

One feature must not introduce its own polling thread, blocking transfer or
per-tick JNI calls unless profiling proves the amortized benefit.

## Work areas and constraints

| Subsystem | Existing implementation in one JAR | In-house replacement boundary | Critical parity requirement |
| --- | --- | --- | --- |
| Chunk generation/I/O | C2ME | Chunk task graph, queue, noise access and safe region I/O | Deterministic world seeds, completed chunks, all structures and existing saves |
| Lighting | ScalableLux | Incremental light propagation, data staging and scheduling | Equivalent block and skylight values after complete updates |
| Rendering | ImmediatelyFast, OBE | Immediate draw batching, transient upload buffers, block-entity mesh caching | Same visible output, correct transparency and animation, no dropped geometry |
| Entity/world ticking | Lithium, Ferrite | Collision/section queries and safe event scheduling | Identical collision and entity ordering, redstone and farm outcomes |
| Memory/resource loading | FerriteCore, ModernFix | Object deduplication, immutable caches and lazy loading | No stale shared mutable objects, same registries and assets |

Native Rust is suitable only for isolated, batch-sized, allocation-heavy
kernels whose data representation and copies make JNI cheaper than Java work.
Java object graphs, renderer command submission and the Minecraft event/tick
scheduler still require JVM integration. Rust is a tool, not an intrinsic speedup.

## Current validation status

- Java/Rust build, all eight embedded module IDs, pinned Minecraft-26.3
  versions, server startup and bundled licenses: verified by CI.
- Deterministic seed parity: **open**. Across completed chunks, the first raw
  comparison flagged differences, including palette-only ordering changes.
  Critically, even *two runs of the vanilla-only baseline* yield different
  generated block contents. Semantic decoding and a repeated-vanilla control
  now distinguish encoding from actual data and expose normal run-to-run
  variance; an inconclusive or failing parity result blocks release.
- Client-side game render tests, end-to-end entity parity, multiple world seeds,
  and measured net FPS/TPS improvement: **not verified**.
- The complete chunk-mesh renderer is not replaced by ImmediatelyFast or OBE.

The branch is not eligible for default release until correctness and
compatibility gates pass. Do not weaken a gate merely to get a green CI run.
