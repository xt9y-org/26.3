/*
 * Adapted from ImmediatelyFast by RK_01/RaphiMC and contributors.
 * https://github.com/RaphiMC/ImmediatelyFast
 *
 * Licensed under LGPL-3.0-or-later. Full license and adapted source
 * are distributed within this mod's JAR.
 */
package dev.xt9y.perf.mixin.rendering;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.opengl.DirectStateAccess;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Apple OpenGL driver path: replace a complete buffer in one upload.
 * Partial updates and non-Apple systems continue to use vanilla bufferSubData.
 *
 * Full uploads retain the identical byte contents and size. No graphics
 * resolution, geometry, material, texture or rendering quality is changed.
 */
@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public abstract class AppleFullBufferUploadMixin {
    private static final boolean XT9Y_APPLE_GL =
            System.getProperty("os.name", "")
                    .toLowerCase(Locale.ROOT).contains("mac");

    @Redirect(
        method = "writeToBuffer",
        at = @At(
            value = "INVOKE",
            target = "Lcom/mojang/renderpearl/backend/opengl/DirectStateAccess;bufferSubData(IJLjava/nio/ByteBuffer;I)V"
        )
    )
    private void xt9y$uploadFullBuffersOnApple(
            DirectStateAccess backend, int buffer, long offset,
            ByteBuffer data, int usage,
            @Local(name = "slice", argsOnly = true) GpuBufferSlice slice) {
        if (XT9Y_APPLE_GL && offset == 0
                && slice.length() == slice.buffer().size()) {
            backend.bufferData(buffer, data, usage);
        } else {
            backend.bufferSubData(buffer, offset, data, usage);
        }
    }
}
