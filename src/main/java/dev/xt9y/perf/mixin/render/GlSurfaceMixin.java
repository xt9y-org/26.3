/*
 * Adapted from ImmediatelyFast (C) 2023-2026 RaphiMC and contributors.
 * LGPL-3.0-or-later; see META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt.
 */
package dev.xt9y.perf.mixin.render;

import com.mojang.renderpearl.backend.opengl.GlConst;
import com.mojang.renderpearl.backend.opengl.GlStateManager;
import com.mojang.renderpearl.backend.opengl.GlSurface;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Rebind default framebuffer before swapping buffers (presentation safety). */
@Mixin(GlSurface.class)
public abstract class GlSurfaceMixin {
    @Inject(method = "present", at = @At("HEAD"))
    private static void xt9y$resetFramebufferForSwap(CallbackInfo ci) {
        GlStateManager._glBindFramebuffer(GlConst.GL_FRAMEBUFFER, 0);
    }
}
