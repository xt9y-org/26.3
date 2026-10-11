package dev.xt9y.perf.mixin.shape;

import it.unimi.dsi.fastutil.objects.Object2BooleanLinkedOpenHashMap;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Reuses the exact full-block shape predicate across repeated queries.
 * Adapted from C2ME (MIT, original copyright 2021-2026 ishland).
 * Thread-local storage avoids shared mutable maps during parallel worldgen.
 */
@Mixin(Block.class)
public abstract class FullBlockShapeMixin {
    @Unique
    private static final ThreadLocal<Object2BooleanLinkedOpenHashMap<VoxelShape>>
            xt9y$fullBlockCache = ThreadLocal.withInitial(
                    () -> new Object2BooleanLinkedOpenHashMap<>(256, 0.25F)
            );

    @Inject(method = "isShapeFullBlock", at = @At("HEAD"), cancellable = true)
    private static void xt9y$cachedFullBlock(VoxelShape shape,
                                               CallbackInfoReturnable<Boolean> cir) {
        Object2BooleanLinkedOpenHashMap<VoxelShape> cache = xt9y$fullBlockCache.get();
        if (cache.containsKey(shape)) {
            cir.setReturnValue(cache.getAndMoveToFirst(shape));
            return;
        }
        boolean result = !Shapes.joinIsNotEmpty(Shapes.block(), shape, BooleanOp.NOT_SAME);
        if (cache.size() >= 256) cache.removeLastBoolean();
        cache.putAndMoveToFirst(shape, result);
        cir.setReturnValue(result);
    }
}
