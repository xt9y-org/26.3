/*
 * Source adaptation of Lithium's ai.pathing PathNavigationRegion hot path.
 * Copyright CaffeineMC contributors; LGPL-3.0-only (see bundled license).
 * https://github.com/CaffeineMC/lithium/blob/develop/common/src/main/java/net/caffeinemc/mods/lithium/mixin/ai/pathing/PathNavigationRegionMixin.java
 */
package dev.xt9y.perf.mixin.pathing;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Cache the navigation region's chunk array in contiguous storage so AI can
 * access blocks without repeated chunk-method dispatch. All positions outside
 * the original region continue to return air, and fluid is derived from state.
 */
@Mixin(PathNavigationRegion.class)
public abstract class PathNavigationRegionMixin implements BlockGetter {
    @Unique private static final BlockState XT9Y_AIR = Blocks.AIR.defaultBlockState();

    @Shadow @Final protected ChunkAccess[][] chunks;
    @Shadow @Final protected int centerX;
    @Shadow @Final protected int centerZ;
    @Shadow public abstract int getMinY();

    @Unique private ChunkAccess[] xt9y$flat;
    @Unique private int xt9y$xLen;
    @Unique private int xt9y$zLen;
    @Unique private int xt9y$minY;
    @Unique private int xt9y$maxY;

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/BlockPos;)V",
            at = @At("RETURN"))
    private void xt9y$flattenChunks(Level world, BlockPos min, BlockPos max, CallbackInfo ci) {
        xt9y$xLen = chunks.length;
        xt9y$zLen = chunks.length == 0 ? 0 : chunks[0].length;
        xt9y$flat = new ChunkAccess[xt9y$xLen * xt9y$zLen];
        for (int x = 0; x < xt9y$xLen; ++x) {
            System.arraycopy(chunks[x], 0, xt9y$flat, x * xt9y$zLen, xt9y$zLen);
        }
        xt9y$minY = this.getMinY();
        xt9y$maxY = this.getMaxY();
    }

    /**
     * @author CaffeineMC contributors; adapted for xt9y
     * @reason Avoid repeated chunk lookup in AI pathfinding; exact state read.
     */
    @Overwrite
    public BlockState getBlockState(BlockPos pos) {
        int y = pos.getY();
        if (y < xt9y$minY || y > xt9y$maxY) return XT9Y_AIR;

        int x = pos.getX();
        int z = pos.getZ();
        int cx = (x >> 4) - centerX;
        int cz = (z >> 4) - centerZ;
        if (cx < 0 || cx >= xt9y$xLen || cz < 0 || cz >= xt9y$zLen) return XT9Y_AIR;

        ChunkAccess chunk = xt9y$flat[cx * xt9y$zLen + cz];
        if (chunk == null) return XT9Y_AIR;
        LevelChunkSection[] sections = chunk.getSections();
        int sy = chunk.getSectionIndex(y);
        if (sy < 0 || sy >= sections.length) return XT9Y_AIR;
        LevelChunkSection section = sections[sy];
        return section != null ? section.getBlockState(x & 15, y & 15, z & 15) : XT9Y_AIR;
    }

    /**
     * @author CaffeineMC contributors; adapted for xt9y
     * @reason Preserve the exact fluid of the returned block state.
     */
    @Overwrite
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos).getFluidState();
    }
}
