package dev.xt9y.perf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerfMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("xt9y263");

    @Override
    public void onInitialize() {
        // Apply a queued operation at startup before a save is opened.
        NativeBridge.initialize(FabricLoader.getInstance().getConfigDir().resolve("xt9y263.conf"));
        final long[] start = {0};
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            start[0] = System.nanoTime();
            NativeBridge.serverStart(start[0]);
        });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (start[0] > 0) NativeBridge.record(3, System.nanoTime() - start[0]);
        });
        ServerChunkEvents.CHUNK_LOAD.register((level, chunk, generated) -> {
            NativeBridge.chunkEvent(0);
            if (generated) NativeBridge.chunkEvent(2);
        });
        ServerChunkEvents.CHUNK_UNLOAD.register((level, chunk) -> NativeBridge.chunkEvent(1));
        LOGGER.info("26.3 Fabric profiler Rust active: {}", NativeBridge.ready());
    }
}
