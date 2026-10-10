package dev.xt9y.perf.client;

import dev.xt9y.perf.NativeBridge;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.type.ImBoolean;

public final class DebugMenu {
    private static final ImBoolean OPEN = new ImBoolean(false);
    private static final int[] QUALITY = {100};
    private static final int[] WORKERS = {Math.max(1,
            Math.min(64, Runtime.getRuntime().availableProcessors()))};
    private static volatile boolean running;
    private static volatile String result = "No benchmark has run.";
    private static String stats = "";
    private static long nextRefreshNs;
    private static boolean initialized;

    private DebugMenu() {}

    public static void toggle() {
        OPEN.set(!OPEN.get());
        if (OPEN.get() && !initialized) {
            QUALITY[0] = NativeBridge.quality();
            initialized = true;
        }
    }

    public static boolean visible() { return OPEN.get(); }

    public static void draw(ImGuiIO io) {
        if (!OPEN.get()) return;
        if (ImGui.begin("26.3 / Rust Performance", OPEN)) {
            ImGui.text("F8: toggle  |  T: release cursor with chat");
            ImGui.separator();
            long now = System.nanoTime();
            if (now >= nextRefreshNs) {
                stats = NativeBridge.snapshot();
                nextRefreshNs = now + 250_000_000L;
            }
            ImGui.textWrapped(stats);
            ImGui.separator();
            ImGui.text("EXPERIMENTAL NATIVE CHUNK KERNEL");
            ImGui.textWrapped("Benchmark only: never modifies Minecraft world chunks. "
                    + "Vanilla terrain, structures, lighting and decorations are unchanged.");
            if (ImGui.sliderInt("Sampling quality (%)", QUALITY, 1, 100)) {
                NativeBridge.setQuality(QUALITY[0]);
            }
            ImGui.sliderInt("Native workers", WORKERS, 1, 64);
            if (!running && ImGui.button("Benchmark 256 chunks")) {
                running = true;
                final int threads = WORKERS[0];
                Thread.startVirtualThread(() -> {
                    try {
                        result = NativeBridge.benchmark(123456789L, 256, threads);
                    } catch (Throwable error) {
                        result = "Benchmark failed: " + error;
                    } finally {
                        running = false;
                    }
                });
            }
            ImGui.textWrapped(running ? "Benchmark running..." : result);
            ImGui.separator();
            ImGui.textWrapped("Live worldgen: VANILLA (100% preserved).");
        }
        ImGui.end();
    }
}
