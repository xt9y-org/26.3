package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import dev.xt9y.perf.engine.randomtick.ChunkRandomSource;
import dev.xt9y.perf.mixin.randomtick.LevelRandomAccessor;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

/**
 * Compare Minecraft's genuine random position generator and our allocation-free
 * generator from precisely the same seed, then restore the original RNG state.
 */
public final class ChunkRandomOracle {
    private ChunkRandomOracle() {}

    public static void verify(ServerLevel world) {
        LevelRandomAccessor access = (LevelRandomAccessor) world;
        ChunkRandomSource fast = (ChunkRandomSource) world;
        BlockPos.MutableBlockPos reused = new BlockPos.MutableBlockPos();
        int original = access.xt9y$getRandValue();
        int comparisons = 0;
        try {
            for (int i = 0; i < 32_768; i++) {
                int x = (i * 173) - 1_000_000;
                int y = (i % 512) - 64;
                int z = (i * -337) + 1_000_000;
                int mask = (1 << (1 + (i % 8))) - 1;
                int start = access.xt9y$getRandValue();

                BlockPos vanilla = access.xt9y$invokeGetBlockRandomPos(x, y, z, mask);
                int expectedState = access.xt9y$getRandValue();

                access.xt9y$setRandValue(start);
                fast.xt9y$getRandomPosInChunk(x, y, z, mask, reused);

                if (!vanilla.equals(reused)
                        || access.xt9y$getRandValue() != expectedState) {
                    throw new AssertionError("Random chunk tick position or RNG mismatch at "
                            + i + ": vanilla=" + vanilla + ", optimized=" + reused);
                }
                comparisons++;
            }
        } finally {
            access.xt9y$setRandValue(original);
        }
        PerfMod.LOGGER.info("Random chunk tick oracle passed: {} exact positions and RNG states",
                comparisons);
    }
}
