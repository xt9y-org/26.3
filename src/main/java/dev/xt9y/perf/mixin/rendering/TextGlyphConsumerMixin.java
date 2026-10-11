/*
 * Adapted from ImmediatelyFast by RK_01/RaphiMC and contributors.
 * https://github.com/RaphiMC/ImmediatelyFast
 *
 * Licensed under the GNU Lesser General Public License, version 3
 * or (at your option) any later version.
 *
 * The implementation is distributed as modifiable Java source in this
 * repository and the production JAR. See META-INF/licenses/LGPL-3.0-ImmediatelyFast.txt.
 */
package dev.xt9y.perf.mixin.rendering;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.feature.TextFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Avoid repeat vertex-buffer lookups for adjacent glyphs sharing render type.
 *
 * Important: identity, not equals(), is used. The cached consumer is scoped to
 * each GlyphRenderer instance, just like the upstream implementation, so no
 * buffer reference is shared between independent text renderers.
 */
@Mixin(targets = "net.minecraft.client.renderer.feature.TextFeatureRenderer$GlyphRenderer")
public abstract class TextGlyphConsumerMixin {
    @Unique private RenderType xt9y$lastRenderType;
    @Unique private VertexConsumer xt9y$lastVertexConsumer;

    @Redirect(
        method = "acceptRenderable",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/renderer/feature/TextFeatureRenderer;getVertexBuilder(Lnet/minecraft/client/renderer/rendertype/RenderType;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
        )
    )
    private VertexConsumer xt9y$cacheGlyphConsumer(
            TextFeatureRenderer renderer, RenderType renderType) {
        if (xt9y$lastRenderType != renderType) {
            xt9y$lastRenderType = renderType;
            xt9y$lastVertexConsumer = renderer.getVertexBuilder(renderType);
        }
        return xt9y$lastVertexConsumer;
    }
}
