package dev.xt9y.perf.engine.blockstate.impl;

import dev.xt9y.perf.engine.blockstate.ducks.FastMapStateHolder;
import dev.xt9y.perf.engine.blockstate.fastmap.FastMap;
import dev.xt9y.perf.engine.blockstate.mixin.accessors.StateHolderAccess;
import net.minecraft.world.level.block.state.StateHolder;

import java.util.Collection;

public final class FastMapStateHolderImpl {
    private FastMapStateHolderImpl() {}

    /** Precompute all transitions from vanilla states without changing their values. */
    public static <S extends StateHolder<?, S>> void initializeFastMap(Collection<S> states) {
        if (states.isEmpty()) return;
        S sample = states.iterator().next();
        FastMap<S> map = new FastMap<>(((StateHolderAccess) sample).getPropertyKeys(), false);
        for (S state : states) {
            int index = map.insertAtIndex(state, S::getValue);
            ((FastMapStateHolder<S>) state).xt9y$setStateMap(map, index);
        }
    }
}
