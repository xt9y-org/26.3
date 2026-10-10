package dev.xt9y.perf.mixin;

import dev.xt9y.perf.client.DebugMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import static cn.enaium.fabric.imgui.FabricImGui.IMGUI;

@Mixin(GameRenderer.class)
public final class GameRendererMixin {
    @Shadow @Final private Minecraft minecraft;

    @Inject(method = "render", at = @At("RETURN"))
    private void xt9y263$renderDebug(CallbackInfo ci) {
        if (!DebugMenu.visible() || !IMGUI.isCreated() || minecraft.player == null) return;
        if (minecraft.gui.screen() != null &&
                !(minecraft.gui.screen() instanceof ChatScreen)) return;
        IMGUI.draw(DebugMenu::draw);
    }
}
