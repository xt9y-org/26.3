package dev.xt9y.perf.mixin;

import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMaps;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.PatchedDataComponentMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Shared-empty-map optimization, adapted from FerriteCore (MIT license).
 * When FerriteCore is also installed, its equivalent patch is disabled via
 * the upstream-supported fabric.mod.json disabled_options metadata.
 */
@Mixin(PatchedDataComponentMap.class)
public abstract class ComponentMapCompactionMixin {
    @Shadow private Reference2ObjectMap<DataComponentType<?>, Optional<?>> patch;
    @Shadow private boolean copyOnWrite;

    @Inject(method = {"applyPatch(Lnet/minecraft/core/component/DataComponentPatch;)V", "restorePatch"},
            at = @At("RETURN"))
    private void xt9y$compactEmptyPatch(CallbackInfo ci) {
        if (patch.isEmpty()) {
            patch = Reference2ObjectMaps.emptyMap();
            copyOnWrite = true;
        }
    }

    @Inject(method = {"set", "remove"}, at = @At("RETURN"))
    private void xt9y$compactAfterMutation(CallbackInfoReturnable<?> ci) {
        xt9y$compactEmptyPatch(ci);
    }
}
