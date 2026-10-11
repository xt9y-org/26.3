/*
 * Source adaptation of Lithium's gen.cached_generator_settings optimization.
 * Copyright CaffeineMC contributors; LGPL-3.0-only (see bundled license).
 * https://github.com/CaffeineMC/lithium/blob/develop/common/src/main/java/net/caffeinemc/mods/lithium/mixin/gen/cached_generator_settings/NoiseBasedChunkGeneratorMixin.java
 */
package dev.xt9y.perf.mixin.worldgen;

import net.minecraft.core.Holder;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/** Lazy, per-generator caching: registry binding is not accessed before first use. */
@Mixin(NoiseBasedChunkGenerator.class)
public abstract class SeaLevelCacheMixin {
    @Shadow @Final private Holder<NoiseGeneratorSettings> settings;
    @Unique private int xt9y$seaLevel = Integer.MIN_VALUE;

    /**
     * @author CaffeineMC contributors, adapted for xt9y
     * @reason The generator's settings holder is immutable after binding;
     *         avoid resolving it on each world-generation call.
     */
    @Overwrite
    public int getSeaLevel() {
        int cached = xt9y$seaLevel;
        if (cached == Integer.MIN_VALUE) {
            cached = this.settings.value().seaLevel();
            xt9y$seaLevel = cached;
        }
        return cached;
    }
}
