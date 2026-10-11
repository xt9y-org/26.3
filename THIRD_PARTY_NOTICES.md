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
