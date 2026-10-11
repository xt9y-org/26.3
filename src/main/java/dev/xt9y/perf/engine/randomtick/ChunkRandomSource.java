package dev.xt9y.perf.engine.randomtick;

import net.minecraft.core.BlockPos;

public interface ChunkRandomSource {
    /**
     * Alternative implementation of {@link Level#getBlockRandomPos(int, int, int, int)} which does not allocate
     * a new {@link BlockPos}.
     */
    void xt9y$getRandomPosInChunk(int x, int y, int z, int mask, BlockPos.MutableBlockPos out);
}
