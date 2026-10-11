package dev.xt9y.perf.mixin.rendering;

import dev.xt9y.perf.engine.PerformanceMode;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Never apply the same redirect twice or interfere with the A/B baseline. */
public final class RenderOptimizationPlugin implements IMixinConfigPlugin {
    private boolean disabled;

    @Override public void onLoad(String pkg) {
        disabled = PerformanceMode.baseline()
                || FabricLoader.getInstance().isModLoaded("immediatelyfast");
    }

    @Override public boolean shouldApplyMixin(String targetClass, String mixinClass) {
        return !disabled;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> other) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
