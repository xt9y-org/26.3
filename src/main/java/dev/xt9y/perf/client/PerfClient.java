package dev.xt9y.perf.client;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xt9y.perf.NativeBridge;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientChunkEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public final class PerfClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("xt9y263", "debug"));

    @Override
    public void onInitializeClient() {
        KeyMapping toggle = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.xt9y263.debug", InputConstants.Type.KEYBOARD,
                InputConstants.KEY_RSHIFT, CATEGORY));
        final long[] start = {0};
        ClientTickEvents.START_CLIENT_TICK.register(client -> start[0] = System.nanoTime());
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (start[0] != 0) NativeBridge.record(2, System.nanoTime() - start[0]);
            while (toggle.consumeClick()) DebugMenu.toggle();
        });
        ClientChunkEvents.CHUNK_LOAD.register((level, chunk) -> NativeBridge.chunkEvent(3));
        ClientChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> NativeBridge.chunkEvent(4));
    }
}
