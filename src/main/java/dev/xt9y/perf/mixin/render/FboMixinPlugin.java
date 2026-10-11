package dev.xt9y.perf.mixin.render;

import dev.xt9y.perf.engine.PerformanceMode;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Explicit opt-in until graphical pixel parity has passed. */
public final class FboMixinPlugin implements IMixinConfigPlugin {
    private boolean enabled;

    @Override public void onLoad(String ignored) {
        enabled = "1".equals(System.getenv("XT9Y_RENDER_FBO_OPT")) &&
                !PerformanceMode.baseline() &&
                !FabricLoader.getInstance().isModLoaded("immediatelyfast");
    }
    @Override public boolean shouldApplyMixin(String target, String name) {
        return enabled;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> mine, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
