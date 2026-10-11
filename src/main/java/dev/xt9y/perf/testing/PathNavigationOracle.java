package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.PathNavigationRegion;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

/**
 * Compare navigation-region state and fluid lookup to the real loaded world.
 * The fixture samples chunk boundaries and negative coordinates.
 */
public final class PathNavigationOracle {
    private PathNavigationOracle() {}

    public static void verify(ServerLevel world) {
        if (world == null) throw new AssertionError("Missing overworld");
        final int minY = world.getMinY();
        final int maxY = world.getMaxY();
        PathNavigationRegion region = new PathNavigationRegion(
                world, new BlockPos(-96, minY, -96), new BlockPos(96, maxY, 96));
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int checked = 0;
        // Sweep every chunk boundary in the 13x13 forced grid and a
        // range of vertical positions including top/bottom sections.
        for (int x = -80; x <= 80; x += 8) {
            for (int z = -80; z <= 80; z += 8) {
                for (int y = minY; y <= maxY; y += 16) {
                    pos.set(x, y, z);
                    BlockState expected = world.getBlockState(pos);
                    BlockState actual = region.getBlockState(pos);
                    if (actual != expected) {
                        throw new AssertionError("Navigation block lookup differs at " + pos
                                + ": actual=" + actual + " expected=" + expected);
                    }
                    FluidState fluid = region.getFluidState(pos);
                    if (fluid != expected.getFluidState()) {
                        throw new AssertionError("Navigation fluid lookup differs at " + pos);
                    }
                    ++checked;
                }
            }
        }
        // Prior sparse sampling skipped the y=seaLevel boundary almost entirely.
        // Exercise the true fluid/air transition at and around the sea surface.
        // This is a block/fluid consistency oracle, not a swimming-AI simulation.
        int seaLevel = world.getSeaLevel();
        int wetPositions = 0;
        int boundaryChecked = 0;
        for (int x = -80; x <= 80; x += 8) {
            for (int z = -80; z <= 80; z += 8) {
                for (int dy = -3; dy <= 3; ++dy) {
                    int y = seaLevel + dy;
                    if (y < minY || y > maxY) continue;
                    pos.set(x, y, z);
                    BlockState expected = world.getBlockState(pos);
                    BlockState actual = region.getBlockState(pos);
                    FluidState expectedFluid = expected.getFluidState();
                    if (actual != expected || region.getFluidState(pos) != expectedFluid) {
                        throw new AssertionError("Sea-surface navigation block/fluid mismatch at "
                                + pos + " block=" + actual + " expected=" + expected);
                    }
                    if (!expectedFluid.isEmpty()) ++wetPositions;
                    ++boundaryChecked;
                }
            }
        }
        if (boundaryChecked < 1000) {
            throw new AssertionError("Insufficient sea-surface navigation samples");
        }
        PerfMod.LOGGER.info(
                "XT9Y_PATH_ORACLE passed={} exact state/fluid lookups, sea_boundary={} wet_positions={}",
                checked, boundaryChecked, wetPositions);
    }
}
