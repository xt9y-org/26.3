package dev.xt9y.perf;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PerfMod implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("xt9y263");

    @Override
    public void onInitialize() {
        NativeBridge.initialize(FabricLoader.getInstance().getConfigDir().resolve("xt9y263.conf"));
        final long[] start = {0};
        ServerTickEvents.START_SERVER_TICK.register(server -> start[0] = System.nanoTime());
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            long previous = start[0];
            if (previous != 0) NativeBridge.observeServerTick(System.nanoTime() - previous);
        });
        LOGGER.info("26.3 native backend: {}", NativeBridge.ready() ? "active" : "unavailable");
    }
}
