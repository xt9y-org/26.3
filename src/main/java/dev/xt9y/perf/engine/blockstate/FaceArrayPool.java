package dev.xt9y.perf.engine.blockstate;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Interns immutable block-state face-sturdiness arrays by content.
 * Adapted from FerriteCore (MIT). No change to the boolean values.
 *
 * Only called when vanilla constructs a new BlockStateBase.Cache. The lock
 * ensures correct reuse if caches are rebuilt on different threads.
 */
public final class FaceArrayPool {
    private static final Map<Key, boolean[]> INTERNED = new HashMap<>();
    private static int reused;

    private FaceArrayPool() {}

    public static synchronized boolean[] intern(boolean[] array) {
        if (array == null) return null;
        Key key = new Key(array);
        boolean[] existing = INTERNED.get(key);
        if (existing != null) {
            reused++;
            return existing;
        }
        INTERNED.put(key, array);
        return array;
    }

    public static synchronized void verify() {
        if (reused <= 0 || INTERNED.isEmpty()) {
            throw new AssertionError("Blockstate face cache compaction never reused any arrays");
        }
    }

    public static synchronized int reusedCount() { return reused; }

    private record Key(boolean[] flags) {
        @Override public int hashCode() { return Arrays.hashCode(flags); }

        @Override public boolean equals(Object other) {
            return other instanceof Key key && Arrays.equals(flags, key.flags);
        }
    }
}
