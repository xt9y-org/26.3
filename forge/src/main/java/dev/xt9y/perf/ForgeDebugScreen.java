package dev.xt9y.perf;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

// Forge uses a native Minecraft screen rather than Fabric's ImGui renderer.
public final class ForgeDebugScreen extends Screen {
    private volatile String result = "No benchmark has run.";
    private volatile boolean running;
    private String stats = "";
    private long refreshAt;
    private int workers = Math.max(1, Math.min(64, Runtime.getRuntime().availableProcessors()));

    public ForgeDebugScreen() {
        super(Component.literal("26.3 / Rust Performance"));
    }

    @Override
    protected void init() {
        int left = (this.width - 250) / 2;
        int top = (this.height - 160) / 2;
        addRenderableWidget(Button.builder(Component.literal("Run 256-chunk benchmark"), button -> {
            if (running) return;
            running = true;
            int selectedWorkers = workers;
            Thread.startVirtualThread(() -> {
                try {
                    result = NativeBridge.benchmark(123456789L, 256, selectedWorkers);
                } finally {
                    running = false;
                }
            });
        }).bounds(left, top + 102, 250, 20).build());

        addRenderableWidget(Button.builder(Component.literal("Workers -"), b ->
                workers = Math.max(1, workers - 1))
                .bounds(left, top + 130, 100, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Workers +"), b ->
                workers = Math.min(64, workers + 1))
                .bounds(left + 150, top + 130, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(graphics, mouseX, mouseY, partialTick);
        long now = System.nanoTime();
        if (now >= refreshAt) {
            stats = NativeBridge.snapshot();
            refreshAt = now + 250_000_000L;
        }
        int left = (this.width - 250) / 2;
        int top = (this.height - 160) / 2;
        graphics.text(this.font, "26.3 / Rust Performance (Forge)", left, top, 0xFFFFFFFF, true);
        int line = top + 16;
        for (String text : stats.split("\\n")) {
            graphics.text(this.font, text, left, line, 0xFFDDDDDD, true);
            line += 12;
        }
        graphics.text(this.font, "Workers: " + workers + "   Sampling quality: " + NativeBridge.quality() + "%",
                left, top + 78, 0xFFFFFFFF, true);
        graphics.text(this.font, running ? "Running benchmark..." : result,
                left, top + 90, 0xFFDDDDDD, true);
    }
}
