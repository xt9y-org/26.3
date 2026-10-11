package dev.xt9y.perf.engine;

/**
 * CI / diagnostic switch; normal builds always load all xt9y optimizations.
 * Resolved once before mixins are selected, never checked inside game hot paths.
 */
public final class PerformanceMode {
    private static final boolean BASELINE =
            "1".equals(System.getenv("XT9Y_PERF_BASELINE"));
    private PerformanceMode() {}
    public static boolean baseline() { return BASELINE; }
}
