package dev.xt9y.perf.mixin;

import dev.xt9y.perf.client.CheatMenu;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep ImGui clicks and scrolling from mining blocks or changing the hotbar. */
@Mixin(MouseHandler.class)
public abstract class CheatMouseMixin {
    @Inject(method = "onButton", at = @At("HEAD"), cancellable = true)
    private void xt9y263$blockWorldClick(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
        if (CheatMenu.visible()) ci.cancel();
    }

    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void xt9y263$blockWorldScroll(long window, double x, double y, CallbackInfo ci) {
        if (CheatMenu.visible()) ci.cancel();
    }
}
