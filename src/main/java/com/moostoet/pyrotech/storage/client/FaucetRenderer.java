package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.fluid.client.FluidBoxRenderer;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.FaucetBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The running faucet's stream: the 1.12 quads, the fluid lying in the spout and falling
 * fifteen sixteenths into the block below (storage sign-off, item 4).
 */
public final class FaucetRenderer implements BlockEntityRenderer<FaucetBlockEntity> {

    private static final float BOTTOM = -15 / 16f;

    public FaucetRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(FaucetBlockEntity faucet, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        FluidStack fluid = faucet.displayed();
        if (!faucet.isActive() || fluid.isEmpty()) {
            return;
        }
        FluidBoxRenderer.Sprites sprites = FluidBoxRenderer.Sprites.of(fluid);
        TextureAtlasSprite still = sprites.still();
        TextureAtlasSprite flowing = sprites.flowing();
        int color = sprites.color();
        VertexConsumer buffer = FluidBoxRenderer.buffer(buffers);
        float fallV = 5 - BOTTOM * 8;
        float frontV = 2 - BOTTOM * 8;
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, faucet.facing());
        // The fluid lying in the spout.
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), px(10), px(16), u(flowing, 5), v(flowing, 0),
            px(6), px(10), px(16), u(flowing, 3), v(flowing, 0),
            px(6), px(10), px(8), u(flowing, 3), v(flowing, 4),
            px(10), px(10), px(8), u(flowing, 5), v(flowing, 4));
        // The foot of the stream.
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), BOTTOM, px(10), u(still, 7), v(still, 0),
            px(6), BOTTOM, px(10), u(still, 3), v(still, 0),
            px(6), BOTTOM, px(8), u(still, 3), v(still, 2),
            px(10), BOTTOM, px(8), u(still, 7), v(still, 2));
        // The four sides of the falling stream.
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), px(10), px(10), u(flowing, 1), v(flowing, 0),
            px(10), px(10), px(8), u(flowing, 0), v(flowing, 0),
            px(10), BOTTOM, px(8), u(flowing, 0), v(flowing, fallV),
            px(10), BOTTOM, px(10), u(flowing, 1), v(flowing, fallV));
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(6), px(10), px(10), u(flowing, 1), v(flowing, 0),
            px(6), px(10), px(8), u(flowing, 0), v(flowing, 0),
            px(6), BOTTOM, px(8), u(flowing, 0), v(flowing, fallV),
            px(6), BOTTOM, px(10), u(flowing, 1), v(flowing, fallV));
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), px(10), px(8), u(flowing, 2), v(flowing, 0),
            px(6), px(10), px(8), u(flowing, 0), v(flowing, 0),
            px(6), BOTTOM, px(8), u(flowing, 0), v(flowing, fallV),
            px(10), BOTTOM, px(8), u(flowing, 2), v(flowing, fallV));
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), px(4), px(10), u(flowing, 2), v(flowing, 0),
            px(6), px(4), px(10), u(flowing, 0), v(flowing, 0),
            px(6), BOTTOM, px(10), u(flowing, 0), v(flowing, frontV),
            px(10), BOTTOM, px(10), u(flowing, 2), v(flowing, frontV));
        // The back of the spout.
        FluidBoxRenderer.quad(pose, buffer, color, light,
            px(10), px(10), px(16), u(flowing, 2), v(flowing, 0),
            px(6), px(10), px(16), u(flowing, 0), v(flowing, 0),
            px(6), px(6), px(16), u(flowing, 0), v(flowing, 2),
            px(10), px(6), px(16), u(flowing, 2), v(flowing, 2));
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(FaucetBlockEntity faucet) {
        BlockPos pos = faucet.getBlockPos();
        return new AABB(pos.getX(), pos.getY() - 1, pos.getZ(), pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1);
    }

    private static float px(int pixels) {
        return FluidBoxRenderer.px(pixels);
    }

    private static float u(TextureAtlasSprite sprite, float pixels) {
        return sprite.getU(pixels / 16);
    }

    private static float v(TextureAtlasSprite sprite, float pixels) {
        return sprite.getV(pixels / 16);
    }
}
