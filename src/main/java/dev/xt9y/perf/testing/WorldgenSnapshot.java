package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import net.minecraft.core.Holder;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fingerprint seeded, force-loaded terrain without changing world state.
 * This is a sampled worldgen oracle, not proof that all chunks are equivalent.
 */
public final class WorldgenSnapshot {
    private WorldgenSnapshot() {}

    public static void verify(ServerLevel level) {
        if (level == null) throw new AssertionError("Missing overworld");
        try {
            // The experimental worldgen source port only replaces this immutable
            // settings getter, so compare it directly to its underlying holder.
            // Independent fresh-world hashes remain diagnostic only: two world
            // startups can be affected by unseeded tick/scheduling order.
            if (level.getChunkSource().getGenerator() instanceof NoiseBasedChunkGenerator generator) {
                Field holderField = NoiseBasedChunkGenerator.class.getDeclaredField("settings");
                holderField.setAccessible(true);
                @SuppressWarnings("unchecked")
                Holder<NoiseGeneratorSettings> holder =
                        (Holder<NoiseGeneratorSettings>)holderField.get(generator);
                int expected = holder.value().seaLevel();
                int actual = generator.getSeaLevel();
                if (actual != expected) {
                    throw new AssertionError("Noise generator sea-level mismatch: " + actual + " vs " + expected);
                }
                PerfMod.LOGGER.info("XT9Y_WORLDGEN_SEA_LEVEL actual={} expected={}", actual, expected);
            } else {
                throw new AssertionError("Worldgen setting oracle requires NoiseBasedChunkGenerator");
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            int count = 0;
            for (int x = -96; x <= 96; x += 8) {
                for (int z = -96; z <= 96; z += 8) {
                    // Subterranean density/cave states are stable after
                    // initial world generation. Surface foliage, snow and
                    // fluid updates can depend on wall-clock ticks even with
                    // the same seed, so do not include them in this oracle.
                    for (int y = level.getMinY(); y <= Math.min(48, level.getMaxY()); y += 16) {
                        pos.set(x, y, z);
                        byte[] state = level.getBlockState(pos).toString()
                                .getBytes(StandardCharsets.UTF_8);
                        digest.update((byte)(state.length >>> 8));
                        digest.update((byte)state.length);
                        digest.update(state);
                        count++;
                    }
                }
            }
            PerfMod.LOGGER.info("XT9Y_WORLDGEN_SNAPSHOT sampled={} sha256={}",
                    count, HexFormat.of().formatHex(digest.digest()));
        } catch (NoSuchAlgorithmException | NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
