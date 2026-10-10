package dev.xt9y.perf.client;

import dev.xt9y.perf.NativeBridge;
import dev.xt9y.perf.PerformanceState;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;

import java.util.Locale;

/** Minimal, allocation-light overlay. All text is cached for half a second. */
public final class DebugMenu {
    private static final ImBoolean OPEN = new ImBoolean(false);
    private static final float[] EMPTY = new float[15];
    private static long nextRefreshNs;
    private static String frameLine = "FPS  measuring...";
    private static String tickLine = "TPS  measuring...";
    private static String chunkLine = "Chunks  measuring...";
    private static String memoryLine = "Heap  measuring...";

    private DebugMenu() {}

    public static void toggle() {
        boolean next = !OPEN.get();
        if (next) NativeBridge.initialize();
        OPEN.set(next);
        PerformanceState.setActive(next && NativeBridge.ready());
        nextRefreshNs = 0;
    }

    public static boolean visible() { return OPEN.get(); }

    private static void refresh() {
        float[] values = NativeBridge.ready() ? NativeBridge.metrics() : EMPTY;
        frameLine = String.format(Locale.ROOT, "FPS  %.0f     Frame  %.1f ms     P95  %.1f ms",
                values[0], values[1], values[13]);
        tickLine = values[5] > 0
                ? String.format(Locale.ROOT, "TPS  %.1f     MSPT  %.1f ms     P95  %.1f ms",
                        values[5], values[4], values[14])
                : "TPS / MSPT  no integrated server samples";
        chunkLine = "Chunks  " + (long) values[8] + " generated   "
                + (long) values[6] + " loaded";
        Runtime runtime = Runtime.getRuntime();
        long used = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
        long max = runtime.maxMemory() / (1024 * 1024);
        memoryLine = "Heap  " + used + " / " + max + " MiB";
    }

    public static void draw(ImGuiIO io) {
        if (!OPEN.get()) return;
        long now = System.nanoTime();
        if (now >= nextRefreshNs) {
            refresh();
            nextRefreshNs = now + 500_000_000L;
        }
        if (ImGui.begin("26.3 Performance", OPEN)) {
            if (NativeBridge.ready()) {
                ImGui.text(frameLine);
                ImGui.text(tickLine);
                ImGui.text(chunkLine);
                ImGui.text(memoryLine);
            } else {
                ImGui.textWrapped("Rust unavailable: " + NativeBridge.error());
            }
        }
        ImGui.end();
        if (!OPEN.get()) PerformanceState.setActive(false);
    }
}
