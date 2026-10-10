package dev.xt9y.perf;

/** Zero-cost-disabled fast-path shared by client, integrated server and mixins. */
public final class PerformanceState {
    private static volatile boolean active;

    private PerformanceState() {}

    public static boolean active() { return active; }
    public static void setActive(boolean enabled) { active = enabled; }
}
