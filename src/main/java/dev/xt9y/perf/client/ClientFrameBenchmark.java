package dev.xt9y.perf.client;

import dev.xt9y.perf.PerfMod;
import java.util.Arrays;

/**
 * In-game real-client frame-time benchmark. Not loaded/active in normal use.
 * Warmup and samples run only with XT9Y_FPS_BENCH=1 and an active world.
 */
public final class ClientFrameBenchmark {
    public static final boolean ACTIVE =
            "1".equals(System.getenv("XT9Y_FPS_BENCH"));
    private static final int WARMUP = 120;
    private static final int MAX = 1800;

    private static final long[] SAMPLES = ACTIVE ? new long[MAX] : null;
    private static int seen;
    private static int size;
    private static long last;
    private static boolean reported;

    private ClientFrameBenchmark() {}

    public static void frame() {
        if (!ACTIVE || reported) return;
        long now = System.nanoTime();
        if (last != 0) {
            long elapsed = now - last;
            if (++seen > WARMUP && size < MAX) SAMPLES[size++] = elapsed;
        }
        last = now;
        if (size == MAX) finish();
    }

    public static void onWorldLeft() {
        if (!ACTIVE || reported) return;
        finish();
        last = 0;
    }

    public static void finish() {
        if (!ACTIVE || reported) return;
        reported = true;
        if (size < 128) {
            PerfMod.LOGGER.warn("XT9Y_FPS_BENCH insufficient world frames: {}", size);
            return;
        }
        long[] sorted = Arrays.copyOf(SAMPLES, size);
        Arrays.sort(sorted);
        double total = 0;
        for (long ns : sorted) total += ns;
        double averageMs = total / (1_000_000.0 * size);
        double p95 = sorted[(int)Math.ceil(size * 0.95) - 1] / 1_000_000.0;
        double p99 = sorted[(int)Math.ceil(size * 0.99) - 1] / 1_000_000.0;
        PerfMod.LOGGER.info(
                "XT9Y_FPS_BENCH frames={} avg_ms={} p95_ms={} p99_ms={} "
                + "avg_fps={} low1pct_fps={} warmup={}",
                size, averageMs, p95, p99, 1000.0 / averageMs,
                1000.0 / p99, WARMUP);
    }
}
