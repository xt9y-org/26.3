package dev.xt9y.perf.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xt9y.perf.NativeBridge;
import dev.xt9y.perf.PerformanceState;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class PerfClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("xt9y263", "debug"));

    @Override
    public void onInitializeClient() {
        if (ClientFrameBenchmark.ACTIVE) {
            net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING
                    .register(mc -> ClientFrameBenchmark.finish());
        }
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xt9y263.debug", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, CATEGORY));
        final long[] start = {0};
        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            start[0] = PerformanceState.active() ? System.nanoTime() : 0;
        });
        final boolean clientSmoke = "1".equals(System.getenv("XT9Y_CLIENT_SMOKE"));
        final boolean[] firstTick = {true};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (CheatMenu.visible() && (client.player == null || client.gui.screen() != null)) {
                CheatMenu.close();
            }
            if (clientSmoke && firstTick[0]) {
                firstTick[0] = false;
                dev.xt9y.perf.PerfMod.LOGGER.info("XT9Y_CLIENT_SMOKE: client ticks running");
            }
            long begin = start[0];
            if (PerformanceState.active() && begin != 0) {
                NativeBridge.record(2, System.nanoTime() - begin);
            }
            while (toggle.consumeClick()) {
                // Minecraft 26.3 uses SDL key constants, not GLFW.
                boolean ctrl = InputConstants.isKeyDown(InputConstants.KEY_LCONTROL)
                        || InputConstants.isKeyDown(InputConstants.KEY_RCONTROL);
                boolean shift = InputConstants.isKeyDown(InputConstants.KEY_LSHIFT);
                boolean alt = InputConstants.isKeyDown(InputConstants.KEY_LALT)
                        || InputConstants.isKeyDown(InputConstants.KEY_RALT);
                if (ctrl && shift && alt && client.player != null && client.gui.screen() == null) {
                    CheatMenu.toggle();
                } else if (!(ctrl && shift && alt)) {
                    DebugMenu.toggle();
                }
            }
        });
    }
}
