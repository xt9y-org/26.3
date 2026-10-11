package dev.xt9y.perf.testing;

import dev.xt9y.perf.PerfMod;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import java.util.Arrays;

/** Opt-in benchmark over actual Minecraft server ticks. Zero cost when disabled. */
public final class ServerTickBenchmark {
    private static final int WARMUP = 100;
    private final long[] samples = new long[2048];
    private int ticks, recorded;
    private long start;

    private ServerTickBenchmark() {}

    public static void register() {
        var recorder = new ServerTickBenchmark();
        ServerTickEvents.START_SERVER_TICK.register(server -> recorder.start = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long duration = System.nanoTime() - recorder.start;
            if (++recorder.ticks > WARMUP && recorder.recorded < recorder.samples.length) {
                recorder.samples[recorder.recorded++] = duration;
            }
        });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> recorder.report());
    }

    private void report() {
        if (recorded < 128) {
            PerfMod.LOGGER.error("XT9Y_TPS_BENCH insufficient samples={}", recorded);
            return;
        }
        long[] sorted = Arrays.copyOf(samples, recorded);
        Arrays.sort(sorted);
        double sum = 0.0;
        for (long elapsed : sorted) sum += elapsed;
        double avg = sum / (1_000_000.0 * recorded);
        double p95 = sorted[(int)Math.ceil(recorded * 0.95) - 1] / 1_000_000.0;
        double p99 = sorted[(int)Math.ceil(recorded * 0.99) - 1] / 1_000_000.0;
        Runtime rt = Runtime.getRuntime();
        long heapMiB = (rt.totalMemory() - rt.freeMemory()) / 1048576L;
        PerfMod.LOGGER.info("XT9Y_TPS_BENCH count={} avg_ms={} p95_ms={} p99_ms={} heap_mib={} warmup={}",
                recorded, avg, p95, p99, heapMiB, WARMUP);
    }
}
