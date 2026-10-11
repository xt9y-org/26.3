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

## Source-port step 6: full-block voxel-shape lookup caching

The full-block shape-cache algorithm has been adapted from the MIT-licensed
C2ME worldgen-vanilla tlcache implementation by ishland et al., with Mojang
Minecraft 26.3 mappings and a plugin to disable the conflicting upstream
Lithium/C2ME mixins:
https://github.com/RelativityMC/C2ME-fabric/blob/dev/26.3.0/c2me-opts-worldgen-vanilla/src/main/java/com/ishland/c2me/opts/worldgen/vanilla/mixin/tlcache/MixinBlock.java

Only boolean results from vanilla's exact voxel-shape comparison are stored in
a per-thread, 256-entry bounded cache. No worldgen, lighting or geometry
approximation is introduced. The C2ME MIT source header and full license are
included with this mod, and no upstream C2ME JAR is bundled.

## Source-port step 7: immediate text vertex lookup

`dev.xt9y.perf.mixin.rendering.TextGlyphConsumerMixin` adapts the
`fast_text_lookup` optimization from ImmediatelyFast (copyright
RK_01/RaphiMC and contributors), LGPL-3.0-or-later:
https://github.com/RaphiMC/ImmediatelyFast/blob/26.3/common/src/main/java/net/raphimc/immediatelyfast/injection/mixins/fast_text_lookup/MixinTextFeatureRenderer_GlyphRenderer.java

Adjacent glyphs with the **same render-type instance** use the same
`VertexConsumer` without repeating `getVertexBuilder`. The consumer
cache belongs to one glyph-renderer instance. No text appearance, glyph
positions, transparency, or GPU quality setting is modified. A Mixin
config plugin disables this feature when ImmediatelyFast is separately
installed or when the user enables the diagnostic baseline.

The full LGPL text and the adapted Java source are packaged within the
distributed JAR at `META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt`
and `META-INF/sources/xt9y-rendering/`.
No ImmediatelyFast binary module is bundled.

## Source-port step 8: Apple GPU full-buffer uploads

`AppleFullBufferUploadMixin` adapts the `fix_slow_buffer_upload_on_apple_gpu`
code from ImmediatelyFast, RK_01/RaphiMC and contributors (LGPL-3.0-or-later):
https://github.com/RaphiMC/ImmediatelyFast/blob/26.3/common/src/main/java/net/raphimc/immediatelyfast/injection/mixins/fix_slow_buffer_upload_on_apple_gpu/MixinGlCommandEncoder.java

Only for macOS, only when a GPU buffer is replaced in full and at offset
zero, invoke `DirectStateAccess.bufferData` rather than
`bufferSubData`. Other code paths are unchanged. This targets the Apple
OpenGL upload bottleneck, not Metal. Client correctness and frame-time
measurements on real Apple hardware remain required.
Full adapted source is embedded at `META-INF/sources/xt9y-rendering/`.

The LGPL-3.0-or-later ImmediatelyFast-derived sources also include the
full GNU GPL version 3 companion text required by LGPLv3, distributed in
`META-INF/licenses/GPL-3.0-Companion.txt`. Both license texts and
modifiable Java source are retained in the universal Fabric JAR.

## Experimental source ports: chunk-generator settings and AI navigation

`SeaLevelCacheMixin` and `PathNavigationRegionMixin` adapt CaffeineMC Lithium's
`gen.cached_generator_settings` and `ai.pathing` implementations. Copyright
CaffeineMC contributors, licensed under LGPL-3.0 (see
`META-INF/licenses/LGPL-3.0-Lithium.txt` and companion GPLv3 text).
Source: https://github.com/CaffeineMC/lithium/tree/develop/common/src/main/java/net/caffeinemc/mods/lithium/mixin

Both remain opt-in behind `XT9Y_NATIVE_HOTPATHS=1` until actual 26.3
client/server compile and gameplay parity pass. They are automatically disabled
in diagnostic baseline mode or when Lithium is installed. No compiled Lithium
JAR is embedded. Adapted modifiable source accompanies the Fabric JAR under
`META-INF/sources/xt9y-engine-hotpaths/`.
