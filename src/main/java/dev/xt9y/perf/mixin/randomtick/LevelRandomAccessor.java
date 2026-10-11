package dev.xt9y.perf.mixin.randomtick;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/** Test-only access to compare exactly with vanilla's RNG coordinate method. */
@Mixin(Level.class)
public interface LevelRandomAccessor {
    @Accessor("randValue") int xt9y$getRandValue();
    @Accessor("randValue") void xt9y$setRandValue(int value);
    @Invoker("getBlockRandomPos")
    BlockPos xt9y$invokeGetBlockRandomPos(int x, int y, int z, int mask);
}
