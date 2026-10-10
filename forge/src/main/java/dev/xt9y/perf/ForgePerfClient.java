package dev.xt9y.perf;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.event.TickEvent;

final class ForgePerfClient {
    private static long clientStart;
    private ForgePerfClient() {}

    static void initialize() {
        TickEvent.ClientTickEvent.Pre.BUS.addListener(event -> clientStart = System.nanoTime());
        TickEvent.ClientTickEvent.Post.BUS.addListener(event -> {
            if (clientStart != 0) {
                NativeBridge.observeClientTick(System.nanoTime() - clientStart);
            }
        });
        InputEvent.Key.BUS.addListener(event -> {
            if (event.getKey() == InputConstants.KEY_F8
                    && event.getAction() == InputConstants.PRESS) {
                var minecraft = Minecraft.getInstance();
                if (minecraft.player != null && minecraft.gui.screen() == null) {
                    minecraft.gui.setScreen(new ForgeDebugScreen());
                }
            }
        });
    }
}
