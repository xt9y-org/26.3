package dev.xt9y.perf.client;

import dev.xt9y.perf.NativeBridge;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

public final class ChunkOperations {
    private ChunkOperations() {}

    public static String queue(int radius) {
        var minecraft = Minecraft.getInstance();
        var server = minecraft.getSingleplayerServer();
        if (server == null || minecraft.player == null) {
            return "Only available in integrated singleplayer.";
        }
        if (!minecraft.player.level().dimension().equals(Level.OVERWORLD)) {
            return "Only Overworld chunks are supported.";
        }
        var position = minecraft.player.chunkPosition();
        return NativeBridge.queueRegeneration(
                server.getWorldPath(LevelResource.ROOT), position.x(), position.z(), radius);
    }
}
