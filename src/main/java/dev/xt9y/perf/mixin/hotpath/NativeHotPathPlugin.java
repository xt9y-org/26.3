package dev.xt9y.perf.mixin.hotpath;

import dev.xt9y.perf.engine.PerformanceMode;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/**
 * Explicit opt-in until real Minecraft gameplay equivalence and A/B benchmarks
 * pass for each hot path. Never collide with Lithium's upstream rewrites.
 */
public final class NativeHotPathPlugin implements IMixinConfigPlugin {
    private boolean apply;

    @Override public void onLoad(String pkg) {
        apply = "1".equals(System.getenv("XT9Y_NATIVE_HOTPATHS"))
                && !PerformanceMode.baseline()
                && !FabricLoader.getInstance().isModLoaded("lithium");
    }

    @Override public boolean shouldApplyMixin(String target, String name) {
        return apply;
    }

    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> own, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
