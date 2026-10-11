/*
 * Adapted from ImmediatelyFast (C) 2023-2026 RaphiMC and contributors.
 * LGPL-3.0-or-later; see META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt.
 */
package dev.xt9y.perf.mixin.render;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.mojang.renderpearl.backend.opengl.GlConst;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps the FBO bound across passes; presentation always resets it to zero. */
@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {
    @WrapWithCondition(method = "submitRenderPass",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/opengl/GlStateManager;_glBindFramebuffer(II)V"))
    private boolean xt9y$avoidUnnecessaryUnbind(int target, int framebuffer) {
        // Mixin plugin only enables this on explicit opt-in.
        return false;
    }

    @Inject(method = "presentTexture", at = @At("HEAD"))
    private void xt9y$resetFramebufferForPresent(CallbackInfo ci) {
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
    }
}
