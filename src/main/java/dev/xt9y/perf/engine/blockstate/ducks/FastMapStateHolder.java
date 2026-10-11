package dev.xt9y.perf.engine.blockstate.ducks;

import dev.xt9y.perf.engine.blockstate.fastmap.FastMap;

public interface FastMapStateHolder<S> {
    void xt9y$setStateMap(FastMap<S> stateMap, int tableIndex);
}
