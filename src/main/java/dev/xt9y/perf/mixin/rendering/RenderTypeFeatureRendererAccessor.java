package dev.xt9y.perf.mixin.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.RenderTypeFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Expose the protected renderer buffer builder via Mixin-generated invoker.
 * This avoids illegal Java access from our separately packaged text redirect.
 */
@Mixin(RenderTypeFeatureRenderer.class)
public interface RenderTypeFeatureRendererAccessor {
    @Invoker("getVertexBuilder")
    VertexConsumer xt9y$getVertexBuilder(RenderType type);
}
