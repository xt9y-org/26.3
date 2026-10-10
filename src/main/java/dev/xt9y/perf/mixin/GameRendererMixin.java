package dev.xt9y.perf.mixin;

import dev.xt9y.perf.NativeBridge;
import dev.xt9y.perf.client.DebugMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import static cn.enaium.fabric.imgui.FabricImGui.IMGUI;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;
    @Unique private long xt9y263$renderStart;
    @Unique private long xt9y263$frameStart;

    @Inject(method = "render", at = @At("HEAD"))
    private void xt9y263$beforeRender(CallbackInfo ci) {
        long now = System.nanoTime();
        if (xt9y263$frameStart != 0) NativeBridge.record(0, now - xt9y263$frameStart);
        xt9y263$frameStart = now;
        xt9y263$renderStart = now;
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void xt9y263$afterRender(CallbackInfo ci) {
        NativeBridge.record(1, System.nanoTime() - xt9y263$renderStart);
        if (!DebugMenu.visible() || !IMGUI.isCreated() || minecraft.player == null) return;
        if (minecraft.gui.screen() != null &&
                !(minecraft.gui.screen() instanceof ChatScreen)) return;
        IMGUI.draw(DebugMenu::draw);
    }
}
