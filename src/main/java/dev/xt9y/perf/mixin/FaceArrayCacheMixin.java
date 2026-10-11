package dev.xt9y.perf.mixin;

import dev.xt9y.perf.engine.blockstate.FaceArrayPool;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Deduplicate equal, immutable cached face-sturdiness arrays.
 * This replaces only that cache field; shape/collision behaviour is untouched.
 * Adapted from FerriteCore's blockstate cache memory optimization (MIT).
 */
@Mixin(targets = "net.minecraft.world.level.block.state.BlockBehaviour$BlockStateBase$Cache")
public abstract class FaceArrayCacheMixin {
    @Shadow @Final @Mutable private boolean[] faceSturdy;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void xt9y$internFaceArray(CallbackInfo ci) {
        faceSturdy = FaceArrayPool.intern(faceSturdy);
    }
}
