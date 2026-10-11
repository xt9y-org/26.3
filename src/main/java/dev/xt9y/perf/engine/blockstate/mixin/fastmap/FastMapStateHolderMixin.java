package dev.xt9y.perf.engine.blockstate.mixin.fastmap;

import dev.xt9y.perf.engine.blockstate.ducks.FastMapStateHolder;
import dev.xt9y.perf.engine.blockstate.fastmap.FastMap;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.block.state.properties.Property;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

/**
 * Replaces large per-state neighbor arrays with a shared, indexed lookup.
 * Ported from FerriteCore (MIT), without its optional property-array removal.
 */
@Mixin(value = StateHolder.class, priority = 900)
public abstract class FastMapStateHolderMixin<O, S> implements FastMapStateHolder<S> {
    @Shadow @Final protected O owner;
    @Shadow public abstract boolean isSingletonState();

    @Unique private int xt9y$stateIndex;
    @Unique private FastMap<S> xt9y$stateTable;

    /**
     * @author malte0811, xt9y adaptation
     * @reason Use an indexed transition table instead of neighbor state arrays.
     */
    @Overwrite
    private <T extends Comparable<T>, V extends T> S setValueInternal(
            Property<T> property, int propertyIndex, V value) {
        int index = property.getInternalIndex(value);
        if (index < 0) {
            throw new IllegalArgumentException("Cannot set property " + property + " to "
                    + value + " on " + owner + ": not an allowed value");
        }
        return xt9y$stateTable.with(xt9y$stateIndex, propertyIndex, index);
    }

    /**
     * @author malte0811, xt9y adaptation
     * @reason Neighbor arrays are replaced after state definitions initialize.
     */
    @Overwrite
    void initializeNeighbors(S[][] neighbors) {
        if (!isSingletonState()) {
            throw new UnsupportedOperationException(
                    "Neighbor arrays replaced by the xt9y shared state lookup");
        }
    }

    @Override
    public void xt9y$setStateMap(FastMap<S> table, int index) {
        xt9y$stateTable = table;
        xt9y$stateIndex = index;
    }
}
