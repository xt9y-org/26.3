package dev.xt9y.perf.mixin.shape;

import dev.xt9y.perf.engine.PerformanceMode;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Avoid competing with Lithium or C2ME's own full-shape caches. */
public final class ShapeMixinPlugin implements IMixinConfigPlugin {
    private boolean upstream;

    @Override public void onLoad(String ignored) {
        var loader = FabricLoader.getInstance();
        upstream = loader.isModLoaded("lithium") ||
                loader.isModLoaded("c2me") ||
                loader.isModLoaded("c2me-opts-worldgen-vanilla");
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return !upstream && !PerformanceMode.baseline();
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
