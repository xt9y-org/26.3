# Source licensing and credit

The in-house Java `ComponentMapCompactionMixin` is adapted from FerriteCore's `PatchedDataComponentMapMixin`, copyright FerriteCore contributors, distributed under the MIT license.

Original source: https://github.com/malte0811/FerriteCore/blob/26.1/Common/src/main/java/malte0811/ferritecore/mixin/datacomponents/PatchedDataComponentMapMixin.java

Full upstream license: `META-INF/licenses/MIT-FerriteCore.txt` in this JAR.

This source optimization replaces the same FerriteCore feature when FerriteCore is installed externally; the metadata requests that upstream disable its `dataComponentPatch` mixin to avoid overlapping injections. Other optimizations in FerriteCore are not incorporated by this source file.

No external performance engine JARs are embedded in this branch.

## Source-port step 2: idle block-entity tick registration

The idle sign/furnace ticker gating and re-registration hooks were ported
from Ferrite (MIT) https://github.com/VoiceLessQ/Ferrite/tree/26.3.x
into our own `dev.xt9y.perf.mixin` source. They target only exact
vanilla block-entity classes, and their lifecycle hooks reinstate
tickers after sign-editor and furnace-inventory transitions. The
`TickerMixinPlugin` disables this mixin set when the upstream
`ferrite` mod is present, preventing two implementations competing
for the same redirect site. See bundled `MIT-Ferrite.txt`.

## Source-port step 3: shared block-state neighbor table

The FerriteCore (MIT) binary and mixed-radix fast map lookup was adapted
into `dev.xt9y.perf.engine.blockstate` and wired into Minecraft's StateDefinition initialization.
No external FerriteCore artifact is required. This implementation retains
vanilla per-state property arrays (only neighbor lookup is replaced) to
minimize mapping risk. If FerriteCore is installed separately, our mixin
plugin skips this feature rather than competing for StateHolder overwrites.
Exact source branch: https://github.com/malte0811/FerriteCore/tree/26.1

## Source-port step 4: blockstate face-array deduplication

This in-house implementation follows FerriteCore's MIT-licensed
BlockStateCacheImpl.deduplicateFaceSturdyArray idea, but uses a small
thread-safe content-key map and limits itself to boolean[] sharing.
It does not intern or mutate VoxelShape internals.
Source: https://github.com/malte0811/FerriteCore/tree/26.1
MIT license is bundled with this mod.

## Source-port step 5: random chunk tick temporary allocations

Files under `dev.xt9y.perf.engine.randomtick` and
`dev.xt9y.perf.mixin.randomtick` are adapted from the
LGPL-3.0-licensed Lithium project by its contributors:
https://github.com/CaffeineMC/lithium/tree/develop/common/src/main/java/net/caffeinemc/mods/lithium/mixin/alloc/chunk_random

The adapted source files and the unmodified LGPL-3.0 license are distributed
inside the JAR at `META-INF/sources/xt9y-chunk-random/` and
`META-INF/licenses/LGPL-3.0-Lithium.txt`, respectively.
No Lithium JAR is bundled. The source implementation uses the exact same
int32 random progression and coordinates as Minecraft but avoids allocations.

## Source-port candidate: redundant framebuffer reset elimination

The `GlCommandEncoderMixin` and `GlSurfaceMixin` are adaptations of
ImmediatelyFast's 26.3 "avoid_redundant_framebuffer_switching" mixins,
copyright (C) 2023-2026 RaphiMC and contributors, licensed LGPL-3.0-or-later.
Source: https://github.com/RaphiMC/ImmediatelyFast/tree/26.3/common/src/main/java/net/raphimc/immediatelyfast/injection/mixins/avoid_redundant_framebuffer_switching

Corresponding .java sources are inside the JAR under
`META-INF/sources/xt9y-render-fbo` and license under
`META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt`.
No ImmediatelyFast JAR is embedded. Disabled by default until
graphical-client equivalence is established.
