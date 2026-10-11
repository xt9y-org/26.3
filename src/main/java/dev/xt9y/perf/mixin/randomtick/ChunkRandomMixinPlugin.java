package dev.xt9y.perf.mixin.randomtick;

import dev.xt9y.perf.engine.PerformanceMode;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import java.util.List;
import java.util.Set;

/** Select either ours or Lithium's random-tick optimization, never both. */
public final class ChunkRandomMixinPlugin implements IMixinConfigPlugin {
    private boolean upstream;
    @Override public void onLoad(String name) {
        upstream = FabricLoader.getInstance().isModLoaded("lithium");
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
