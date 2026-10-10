package dev.xt9y.perf.client;

import dev.xt9y.perf.NativeBridge;
import dev.xt9y.perf.PerformanceState;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;

/** One small profiler window: frame, tick, chunks and memory. */
public final class DebugMenu {
    private static final ImBoolean OPEN = new ImBoolean(false);
    private static final float[] EMPTY = new float[15];
    private static float[] metrics = EMPTY;
    private static long nextRefreshNs;

    private DebugMenu() {}

    public static void toggle() {
        boolean next = !OPEN.get();
        if (next) NativeBridge.initialize();
        OPEN.set(next);
        PerformanceState.setActive(next && NativeBridge.ready());
        nextRefreshNs = 0;
    }

    public static boolean visible() { return OPEN.get(); }

    public static void draw(ImGuiIO io) {
        if (!OPEN.get()) return;
        long now = System.nanoTime();
        if (now >= nextRefreshNs) {
            metrics = NativeBridge.metrics();
            nextRefreshNs = now + 500_000_000L;
        }

        if (ImGui.begin("26.3 Performance", OPEN)) {
            if (!NativeBridge.ready()) {
                ImGui.textWrapped("Rust profiler unavailable: " + NativeBridge.error());
            } else {
                ImGui.text(String.format(java.util.Locale.ROOT,
                        "FPS  %.0f    Frame  %.1f ms    P95  %.1f ms",
                        metrics[0], metrics[1], metrics[13]));
                if (metrics[5] > 0) {
                    ImGui.text(String.format(java.util.Locale.ROOT,
                            "TPS  %.1f    MSPT  %.1f ms    P95  %.1f ms",
                            metrics[5], metrics[4], metrics[14]));
                } else {
                    ImGui.text("TPS / MSPT  n/a (no local server samples)");
                }
                ImGui.text(String.format(java.util.Locale.ROOT,
                        "Chunks  %d generated   %d loaded",
                        (long) metrics[8], (long) metrics[6]));

                Runtime runtime = Runtime.getRuntime();
                long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
                long max = runtime.maxMemory() / (1024 * 1024);
                ImGui.text("Heap  " + used + " / " + max + " MiB");
            }
        }
        ImGui.end();
        if (!OPEN.get()) PerformanceState.setActive(false);
    }
}
