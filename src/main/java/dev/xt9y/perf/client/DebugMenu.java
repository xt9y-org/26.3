package dev.xt9y.perf.client;

import dev.xt9y.perf.NativeBridge;
import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.ImVec2;
import imgui.type.ImBoolean;

/** Multi-page diagnostic window backed by native Rust samples. */
public final class DebugMenu {
    private static final ImBoolean OPEN = new ImBoolean(false);
    private static final String[] TABS = {"Overview", "FPS / Frames", "TPS / Ticks", "Chunks", "Tools"};
    private static int activeTab;
    private static final int[] RADIUS = {0};
    private static boolean confirm;
    private static String message = "No regeneration has been queued.";
    private static float[] values = new float[13];
    private static float[] frameMs = new float[0];
    private static float[] fps = new float[0];
    private static float[] serverMs = new float[0];
    private static float[] clientMs = new float[0];
    private static long refreshAt;

    private DebugMenu() {}
    public static void toggle() { OPEN.set(!OPEN.get()); }
    public static boolean visible() { return OPEN.get(); }

    private static void refresh() {
        long now = System.nanoTime();
        if (now < refreshAt) return;
        refreshAt = now + 250_000_000L;
        values = NativeBridge.metrics();
        frameMs = NativeBridge.history(0);
        serverMs = NativeBridge.history(3);
        clientMs = NativeBridge.history(2);
        fps = new float[frameMs.length];
        for (int i = 0; i < frameMs.length; ++i) {
            fps[i] = frameMs[i] > 0 ? Math.min(500f, 1000f / frameMs[i]) : 0;
        }
    }

    private static String decimal(float value) {
        return String.format(java.util.Locale.ROOT, "%.2f", value);
    }

    private static void menu() {
        for (int i = 0; i < TABS.length; ++i) {
            if (i > 0) ImGui.sameLine();
            if (ImGui.button((activeTab == i ? "[ " : "") + TABS[i]
                    + (activeTab == i ? " ]" : "") + "##page" + i)) activeTab = i;
        }
        ImGui.separator();
    }

    private static void graph(String title, float[] history, float max) {
        if (history.length < 2) {
            ImGui.textDisabled(title + " — collecting measurements...");
            return;
        }
        ImGui.plotLines(title, history, history.length, 0, "", 0f, max, 520f, 85f);
    }

    private static void pie(String title, float measured, float budget, String label) {
        ImGui.text(title);
        ImVec2 position = ImGui.getCursorScreenPos();
        float cx = position.x + 54f;
        float cy = position.y + 54f;
        float r = 50f;
        float fraction = budget <= 0 ? 0 : Math.clamp(measured / budget, 0f, 1f);
        var draw = ImGui.getWindowDrawList();
        for (int i = 0; i < 72; i++) {
            float begin = (float) (-Math.PI / 2 + i * (Math.PI * 2 / 72));
            float end = (float) (-Math.PI / 2 + (i + 1) * (Math.PI * 2 / 72));
            int color = i / 72f < fraction ? 0xFFCE994F : 0xFF555C6B;
            draw.addTriangleFilled(cx, cy,
                    cx + r * (float)Math.cos(begin), cy + r * (float)Math.sin(begin),
                    cx + r * (float)Math.cos(end), cy + r * (float)Math.sin(end), color);
        }
        ImGui.dummy(112f, 112f);
        ImGui.text(label + ": " + decimal(measured) + " ms");
        ImGui.text("Remaining / unattributed: " + decimal(Math.max(0f, budget - measured)) + " ms");
        if (measured > budget) ImGui.text("Over budget: " + decimal(measured - budget) + " ms");
    }

    private static void overview() {
        ImGui.text("Live Rust profiling " + (NativeBridge.ready() ? "ACTIVE" : "UNAVAILABLE"));
        ImGui.text("FPS  " + decimal(values[0]) + "     Frame  " + decimal(values[1]) + " ms");
        ImGui.text("Renderer CPU  " + decimal(values[2]) + " ms    Client tick  " + decimal(values[3]) + " ms");
        ImGui.text("TPS  " + decimal(values[5]) + "     Server MSPT  " + decimal(values[4]) + " ms");
        ImGui.text("Chunk loads  " + (long)values[6]
                + "     Newly generated  " + (long)values[8]);
        graph("FPS history", fps, 240f);
        graph("Server tick milliseconds", serverMs, 80f);
        ImGui.textWrapped("These are measured frame, tick and chunk lifecycle statistics, "
                + "not synthetic generation throughput.");
    }

    private static void frames() {
        ImGui.text("FPS  " + decimal(values[0]) + "   /   Frame interval  " + decimal(values[1]) + " ms");
        graph("FPS (recent history)", fps, 240f);
        graph("Frame interval (ms)", frameMs, 60f);
        pie("Frame wall-time breakdown", values[2], values[1], "CPU renderer");
        ImGui.textWrapped("The remainder includes game logic, driver waits and other work. "
                + "GPU timings and per-pass allocations are not measured yet.");
    }

    private static void ticks() {
        ImGui.text("TPS  " + decimal(values[5]) + "   /   MSPT  " + decimal(values[4]) + " ms");
        graph("Server tick duration (ms)", serverMs, 80f);
        graph("Client tick duration (ms)", clientMs, 40f);
        pie("20 TPS budget: 50 ms per tick", values[4], 50f, "Measured server tick work");
        ImGui.textWrapped("This pie shows time spent inside the tick versus remaining "
                + "budget. It does not pretend to identify entities, worldgen, or networking.");
    }

    private static void chunks() {
        ImGui.text("Actual vanilla Fabric chunk lifecycle events (since startup)");
        ImGui.separator();
        ImGui.text("Server chunks loaded: " + (long)values[6]);
        ImGui.text("Server chunks unloaded: " + (long)values[7]);
        ImGui.text("Newly generated chunks: " + (long)values[8]);
        ImGui.text("Client chunks loaded: " + (long)values[9]);
        ImGui.text("Client chunks unloaded: " + (long)values[10]);
        ImGui.separator();
        ImGui.textWrapped("Chunk generation counts are real server events. Individual "
                + "noise, biome, structures, lighting, and storage timings are not yet "
                + "available until those Minecraft stages are instrumented.");
    }

    private static void tools() {
        ImGui.text("Overworld chunk regeneration (integrated singleplayer)");
        ImGui.textWrapped("Destructive: built structures, inventory containers, entities "
                + "and POI data inside selected chunks will be deleted. Vanilla "
                + "regenerates terrain and lighting when those chunks next load.");
        ImGui.sliderInt("Chunk radius", RADIUS, 0, 2);
        int count = (2 * RADIUS[0] + 1) * (2 * RADIUS[0] + 1);
        ImGui.text("Selected: " + count + " chunk(s) around your current position.");
        if (!confirm) {
            if (ImGui.button("Review regeneration")) confirm = true;
        } else {
            ImGui.textWrapped("Queue deletion of " + count + " chunks? "
                    + "Close Minecraft completely, then restart. A backup is made first.");
            if (ImGui.button("CONFIRM - queue for next game launch")) {
                message = ChunkOperations.queue(RADIUS[0]);
                confirm = false;
            }
            ImGui.sameLine();
            if (ImGui.button("Cancel")) confirm = false;
        }
        ImGui.separator();
        ImGui.textWrapped(message);
        ImGui.textWrapped("Native status: " + NativeBridge.regenerationStatus());
        ImGui.textWrapped("Regeneration cannot be performed in a loaded save or multiplayer world.");
    }

    public static void draw(ImGuiIO io) {
        if (!OPEN.get()) return;
        refresh();
        if (ImGui.begin("26.3 | Fabric Performance", OPEN)) {
            ImGui.text("RIGHT SHIFT: toggle     T: release cursor through chat");
            menu();
            switch (activeTab) {
                case 0 -> overview();
                case 1 -> frames();
                case 2 -> ticks();
                case 3 -> chunks();
                case 4 -> tools();
                default -> activeTab = 0;
            }
        }
        ImGui.end();
    }
}
