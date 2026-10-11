# Minecraft 26.3 Performance — v1.0.0 Release Candidate

This is a **major version packaging and instrumentation milestone**, not an
unverified claim to replace every Minecraft engine. No third-party performance
JARs are included in this candidate. Minecraft **26.3**, Fabric Loader and
Fabric API are still required.

## Native source-level features included

- Empty item-component map compaction, block-state indexed lookup and shared
  face arrays, with vanilla-equivalence oracles.
- Idle sign/furnace ticker avoidance and chunk random-position allocation
  reduction; exact RNG sequence covered by CI.
- Bounded voxel-shape boolean cache.
- Immediate text glyph-buffer lookup and macOS full-buffer OpenGL upload
  optimization; compile and graphical boot tests still must execute.
- Native profiler (FPS, MSPT, heap, chunk events), disabled until opened.
- Real opt-in FPS and server MSPT benchmarking support.

Additional worldgen sea-level and navigation-region chunk access optimizations
are **experimental and off by default**. Enable only for testing with
`XT9Y_NATIVE_HOTPATHS=1` after preserving a backup of your world.

## Not included as complete engine rewrites

This version does **not** contain the full C2ME parallel chunk scheduler,
ScalableLux block/skylight engine, Sodium terrain renderer, or comprehensive
Lithium entity/pathfinding engine. Each would require large source/API ports and
world/visual equivalence tests. The experimental eight-module bundle stays
separate because its world output was not equivalent.

## Before publishing

From the repository root on macOS or Linux (Java 25, Rust, Gradle, Python):

```sh
bash scripts/test_release_candidate.sh
```

The script builds a version `1.0.0` candidate, runs unit tests and seeded
Minecraft server correctness/lighting/generator-settings/navigation checks, and performs
one real MSPT A/B run in temporary worlds. It leaves logs under `run/`.
**It does not verify GPU pixel parity or modpack-wide compatibility.**

Also test a copy of your own world with the same gameplay/mod configuration,
including hostile/passive mob navigation, dynamic lights, text rendering,
chunk generation and exploration. Compare native baseline with
`XT9Y_PERF_BASELINE=1`, then again with optimizations active. On Apple
hardware, compare frame times before/after at the same scene; the headless
Linux software-OpenGL smoke is not a substitute for this.

## Publish a stable GitHub release

Only after the candidate passes your validation and is merged to **main**:

1. Open **Actions → Universal Fabric release → Run workflow**.
2. Select **main**, set `version` to `1.0.0`, and
   `confirm_publish` to **RELEASE**, and
   `hardware_client_tested` to **TESTED** after personally verifying the
   client on a real GPU. The CI Mesa software Vulkan render can be
   inconclusive, but actual Mixin errors still fail publication.
3. The workflow compiles Windows x64, Linux x64, macOS Apple Silicon and Intel
   macOS native libraries, then runs server oracles and the graphical client
   smoke. A failure in any required job stops publication.
4. On success the workflow creates stable tag **v1.0.0** and publishes
   **one universal Fabric JAR**, `SHA256SUMS.txt` and release notes.

A normal push to main still creates a **dev prerelease**. A manual run with a
version or confirmation error does not publish. Do not reuse an existing
stable tag as the target version.

If GitHub-hosted jobs fail before step 1 / have no assigned runner, restore
organization Actions runners or billing limits before running this workflow.
Those infrastructure failures are not evidence of a passing test.

## Known verification limitations

A fresh-world sampled block-state hash can differ between independent world
startups, including at subterranean positions. The release gate therefore
requires **exact cached sea-level equivalence** and **exact navigation-region
block/fluid lookups**, while reporting a differing whole-world hash as
**INCONCLUSIVE**. This is not a full chunk-generation equivalence proof.
Set `XT9Y_REQUIRE_WORLD_HASH=1` for a strict, potentially failing experiment.

The Ubuntu 26.3 SDL3 headless graphical smoke can initialize Mesa Lavapipe
Vulkan but be unable to render the title frame within the software GPU time
budget. Such runs are marked **INCONCLUSIVE**, never a graphical success;
a stable release requires your explicit `hardware_client_tested=TESTED`
attestation after testing on real hardware. Actual shader/output visual
parity and real GPU FPS cannot be inferred from software Vulkan.
