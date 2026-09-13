package com.moostoet.pyrotech.library.fluid.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Draws fluid quads from a fluid's own sprites, the way the 1.12 tank, faucet, barrel,
 * soaking pot, crucible, oil lamp, and tar collector renderers did with the tessellator.
 * Quads go on the translucent entity layer over the block atlas, lit by the lightmap only,
 * with both faces drawn as the 1.12 renderers drew them.
 */
public final class FluidBoxRenderer {

    private static final float PX = 1 / 16f;

    private FluidBoxRenderer() {
    }

    /** A fluid's still and flowing sprites and its tint. */
    public record Sprites(TextureAtlasSprite still, TextureAtlasSprite flowing, int color) {

        public static Sprites of(FluidStack fluid) {
            IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid.getFluid());
            var atlas = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
            return new Sprites(atlas.apply(extensions.getStillTexture(fluid)), atlas.apply(extensions.getFlowingTexture(fluid)),
                extensions.getTintColor(fluid));
        }
    }

    public static VertexConsumer buffer(MultiBufferSource buffers) {
        return buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
    }

    /**
     * A column of fluid inset from the block's faces, {@code bottom} to {@code top} in
     * block units: the top face and the four sides. The sides take the still texture per
     * block of height, with a partial block showing the texture's lower part, as 1.12 did.
     */
    public static void column(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite still, int color, int light,
                              float inset, float bottom, float top) {
        float min = inset;
        float max = 1 - inset;
        top(pose, buffer, still, color, light, min, min, max, max, top);
        for (int block = (int) Math.floor(bottom); block < top; block++) {
            float yMin = Math.max(block, bottom);
            float yMax = Math.min(block + 1, top);
            float vMax = still.getV(1);
            float vMin = still.getV(1 - (yMax - block));
            quad(pose, buffer, color, light,
                max, yMin, min, still.getU0(), vMax,
                max, yMax, min, still.getU0(), vMin,
                max, yMax, max, still.getU1(), vMin,
                max, yMin, max, still.getU1(), vMax);
            quad(pose, buffer, color, light,
                min, yMin, min, still.getU0(), vMax,
                min, yMin, max, still.getU1(), vMax,
                min, yMax, max, still.getU1(), vMin,
                min, yMax, min, still.getU0(), vMin);
            quad(pose, buffer, color, light,
                min, yMin, max, still.getU0(), vMax,
                max, yMin, max, still.getU1(), vMax,
                max, yMax, max, still.getU1(), vMin,
                min, yMax, max, still.getU0(), vMin);
            quad(pose, buffer, color, light,
                min, yMin, min, still.getU0(), vMax,
                min, yMax, min, still.getU0(), vMin,
                max, yMax, min, still.getU1(), vMin,
                max, yMin, min, still.getU1(), vMax);
        }
    }

    /** A horizontal face at height {@code y} covering x0..x1 by z0..z1, textured by position. */
    public static void top(PoseStack pose, VertexConsumer buffer, TextureAtlasSprite sprite, int color, int light,
                           float x0, float z0, float x1, float z1, float y) {
        quad(pose, buffer, color, light,
            x0, y, z0, sprite.getU(x0), sprite.getV(z0),
            x0, y, z1, sprite.getU(x0), sprite.getV(z1),
            x1, y, z1, sprite.getU(x1), sprite.getV(z1),
            x1, y, z0, sprite.getU(x1), sprite.getV(z0));
    }

    /** One quad, positions in block units and texture coordinates in sprite fractions. */
    public static void quad(PoseStack pose, VertexConsumer buffer, int color, int light,
                            float x1, float y1, float z1, float u1, float v1,
                            float x2, float y2, float z2, float u2, float v2,
                            float x3, float y3, float z3, float u3, float v3,
                            float x4, float y4, float z4, float u4, float v4) {
        PoseStack.Pose last = pose.last();
        vertex(buffer, last, x1, y1, z1, u1, v1, color, light);
        vertex(buffer, last, x2, y2, z2, u2, v2, color, light);
        vertex(buffer, last, x3, y3, z3, u3, v3, color, light);
        vertex(buffer, last, x4, y4, z4, u4, v4, color, light);
    }

    /** The 1.12 {@code getInterpolatedU(px)}: a texture coordinate from a pixel count. */
    public static float px(int pixels) {
        return pixels * PX;
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, float u, float v,
                               int color, int light) {
        buffer.addVertex(pose, x, y, z)
            .setColor(color | 0xFF000000)
            .setUv(u, v)
            .setOverlay(OverlayTexture.NO_OVERLAY)
            .setLight(light)
            .setNormal(pose, 0, 1, 0);
    }
}
