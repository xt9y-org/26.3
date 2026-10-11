package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Test-only fixture: compare real block and sky light after propagation settles.
 * Never use fixed tick delays for asynchronous lighting: overloaded chunkgen can
 * postpone updates, making an unchanged stale field appear to be a regression.
 */
public final class LightingOracle {
    private static final BlockPos EMITTER = new BlockPos(0, 260, 0);
    private static final BlockPos NEIGHBOR = new BlockPos(1, 260, 0);
    private static final BlockPos.MutableBlockPos MUTABLE = new BlockPos.MutableBlockPos();
    private int ticks, settled;
    private String illuminated;
    private boolean running, removed, finished;

    private LightingOracle() {}

    public static void register() {
        var oracle = new LightingOracle();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> oracle.running = true);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!oracle.running || oracle.finished) return;
            ServerLevel level = server.overworld();
            if (level == null) return;
            int tick = ++oracle.ticks;
            if (tick == 1) {
                level.setBlock(EMITTER, Blocks.GLOWSTONE.defaultBlockState(), 3);
                for (int y = 257; y <= 263; y++)
                    for (int z = -3; z <= 3; z++)
                        level.setBlock(new BlockPos(4, y, z),
                                Blocks.OBSIDIAN.defaultBlockState(), 3);
            }

            // Observe normal asynchronous Minecraft propagation. Never
            // force-drain the light engine from this test callback.

            if (!oracle.removed) {
                if (tick >= 12
                        && level.getBrightness(LightLayer.BLOCK, EMITTER) == 15
                        && level.getBrightness(LightLayer.BLOCK, NEIGHBOR) >= 14) {
                    oracle.illuminated = oracle.snapshot(level);
                    level.setBlock(EMITTER, Blocks.AIR.defaultBlockState(), 3);
                    oracle.removed = true;
                    oracle.settled = 0;
                }
                return;
            }

            boolean extinguished =
                    level.getBrightness(LightLayer.BLOCK, EMITTER) == 0
                    && level.getBrightness(LightLayer.BLOCK, NEIGHBOR) == 0;
            oracle.settled = extinguished ? oracle.settled + 1 : 0;

            // Require successive stable observations; timing can vary wildly
            // during chunkgen, but the final light values must not.
            if (oracle.settled >= 8) {
                String dark = oracle.snapshot(level);
                if (oracle.illuminated.equals(dark)) {
                    throw new AssertionError("Settled light output identical after removing source");
                }
                PerfMod.LOGGER.info("XT9Y_LIGHT_ORACLE lit={} dark={} positions={}",
                        oracle.illuminated, dark, 25 * 13 * 25);
                oracle.finished = true;
            } else if (tick == 400) {
                PerfMod.LOGGER.error(
                        "XT9Y_LIGHT_ORACLE timed out: removed={} sourceLight={} nearLight={}",
                        oracle.removed,
                        level.getBrightness(LightLayer.BLOCK, EMITTER),
                        level.getBrightness(LightLayer.BLOCK, NEIGHBOR));
            }
        });
    }

    private String snapshot(ServerLevel level) {
        try {
            MessageDigest hash = MessageDigest.getInstance("SHA-256");
            for (int y = 254; y <= 266; y++)
                for (int z = -12; z <= 12; z++)
                    for (int x = -12; x <= 12; x++) {
                        MUTABLE.set(x, y, z);
                        int block = level.getBrightness(LightLayer.BLOCK, MUTABLE);
                        int sky = level.getBrightness(LightLayer.SKY, MUTABLE);
                        hash.update((byte)((block << 4) | sky));
                    }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
