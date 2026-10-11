# Source licensing and credit

The in-house Java `ComponentMapCompactionMixin` is adapted from FerriteCore's `PatchedDataComponentMapMixin`, copyright FerriteCore contributors, distributed under the MIT license.

Original source: https://github.com/malte0811/FerriteCore/blob/26.1/Common/src/main/java/malte0811/ferritecore/mixin/datacomponents/PatchedDataComponentMapMixin.java

Full upstream license: `META-INF/licenses/MIT-FerriteCore.txt` in this JAR.

This source optimization replaces the same FerriteCore feature when FerriteCore is installed externally; the metadata requests that upstream disable its `dataComponentPatch` mixin to avoid overlapping injections. Other optimizations in FerriteCore are not incorporated by this source file.

No external performance engine JARs are embedded in this branch.
