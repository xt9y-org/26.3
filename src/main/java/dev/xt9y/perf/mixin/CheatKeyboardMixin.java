package dev.xt9y.perf.mixin;

import com.mojang.blaze3d.platform.InputConstants;
import dev.xt9y.perf.client.CheatMenu;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ImGui owns text entry while the cheats window is open; retain key releases and toggle. */
@Mixin(KeyboardHandler.class)
public abstract class CheatKeyboardMixin {
    @Inject(method = "keyPress", at = @At("HEAD"), cancellable = true)
    private void xt9y263$blockGameKeys(long window, int action, KeyEvent event, CallbackInfo ci) {
        if (!CheatMenu.visible()) return;
        if (event.key() == InputConstants.KEY_RSHIFT) return;
        if (event.key() == InputConstants.KEY_ESCAPE && action == InputConstants.PRESS) {
            CheatMenu.close();
            ci.cancel();
            return;
        }
        // Keep release events so vanilla keys don't remain stuck after closing.
        if (action != InputConstants.RELEASE) ci.cancel();
    }
}
