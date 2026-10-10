package dev.xt9y.perf;

import java.nio.file.Path;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod("xt9y263")
public final class ForgePerfMod {
    private long serverStart;

    public ForgePerfMod() {
        NativeBridge.initialize(Path.of("config", "xt9y263.conf"));

        TickEvent.ServerTickEvent.Pre.BUS.addListener(event -> serverStart = System.nanoTime());
        TickEvent.ServerTickEvent.Post.BUS.addListener(event -> {
            if (serverStart != 0) {
                NativeBridge.observeServerTick(System.nanoTime() - serverStart);
            }
        });

        if (FMLEnvironment.dist == Dist.CLIENT) {
            ForgePerfClient.initialize();
        }
    }
}
