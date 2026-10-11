package dev.xt9y.perf.mixin;

import dev.xt9y.perf.engine.PerformanceMode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** Keep the profiler active but disable gameplay optimizations in oracle baseline. */
public final class PerformanceMixinPlugin implements IMixinConfigPlugin {
    @Override public void onLoad(String mixinPackage) {}
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return !mixin.endsWith("ComponentMapCompactionMixin") || !PerformanceMode.baseline();
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
