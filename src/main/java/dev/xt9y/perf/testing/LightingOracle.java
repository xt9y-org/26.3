package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.server.level.ServerLevel;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Test-only world fixture; measures real vanilla block and sky light propagation. */
public final class LightingOracle {
    private static final BlockPos EMITTER = new BlockPos(0, 260, 0);
    private static final BlockPos.MutableBlockPos MUTABLE = new BlockPos.MutableBlockPos();
    private int ticks;
    private String illuminated;
    private String darkened;

    private LightingOracle() {}

    public static void register() {
        var oracle = new LightingOracle();
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel level = server.overworld();
            if (level == null) return;
            int tick = ++oracle.ticks;
            if (tick == 10) {
                // An occluding wall plus a source entirely in loaded spawn chunk.
                level.setBlock(EMITTER, Blocks.GLOWSTONE.defaultBlockState(), 3);
                for (int y = 257; y <= 263; y++)
                    for (int z = -3; z <= 3; z++)
                        level.setBlock(new BlockPos(4, y, z),
                                Blocks.OBSIDIAN.defaultBlockState(), 3);
            } else if (tick == 80) {
                oracle.illuminated = oracle.snapshot(level);
                int emitted = level.getBrightness(LightLayer.BLOCK, EMITTER);
                if (emitted != 15) {
                    throw new AssertionError("Glowstone emission expected 15, got " + emitted);
                }
                level.setBlock(EMITTER, Blocks.AIR.defaultBlockState(), 3);
            } else if (tick == 150) {
                oracle.darkened = oracle.snapshot(level);
                if (oracle.illuminated.equals(oracle.darkened)) {
                    throw new AssertionError("Light removal did not change the light field");
                }
                PerfMod.LOGGER.info("XT9Y_LIGHT_ORACLE lit={} dark={} positions={}",
                        oracle.illuminated, oracle.darkened, 25 * 13 * 25);
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
                        hash.update((byte) ((block << 4) | sky));
                    }
            return HexFormat.of().formatHex(hash.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
