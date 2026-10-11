package dev.xt9y.perf.engine.blockstate.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/** FerriteCore provides the same neighbor optimization; never apply both. */
public final class BlockstateMixinPlugin implements IMixinConfigPlugin {
    private boolean ferriteCoreInstalled;
    @Override public void onLoad(String mixinPackage) {
        ferriteCoreInstalled = FabricLoader.getInstance().isModLoaded("ferritecore");
    }
    @Override public boolean shouldApplyMixin(String target, String mixin) {
        return !ferriteCoreInstalled;
    }
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> my, Set<String> others) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
    @Override public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
