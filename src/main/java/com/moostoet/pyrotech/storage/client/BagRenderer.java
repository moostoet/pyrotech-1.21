package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.fluid.client.FluidBoxRenderer;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.BagBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The bag's fill: a plane of gravel rising with its contents, the held item's ghost over
 * the opening, and the total count.
 */
public final class BagRenderer implements BlockEntityRenderer<BagBlockEntity> {

    private static final ResourceLocation FILL_TEXTURE = ResourceLocation.withDefaultNamespace("block/gravel");
    private static final float FILL_MIN_X = 4 / 16f;
    private static final float FILL_MAX_X = 12 / 16f;
    private static final float FILL_MIN_Z = 6 / 16f;
    private static final float FILL_MAX_Z = 10 / 16f;
    private static final float FILL_BOTTOM = 1 / 16f;
    private static final float FILL_HEIGHT = 7 / 16f;
    private static final float GHOST_Y = 10 / 16f;
    private static final float GHOST_SCALE = 0.5f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.1, 0);

    public BagRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(BagBlockEntity bag, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = bag.facing();
        int count = bag.handler().totalCount();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        if (count > 0) {
            TextureAtlasSprite sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(FILL_TEXTURE);
            VertexConsumer buffer = buffers.getBuffer(Sheets.solidBlockSheet());
            float level = FILL_BOTTOM + FILL_HEIGHT * count / bag.block().capacity();
            FluidBoxRenderer.top(pose, buffer, sprite, 0xFFFFFF, light, FILL_MIN_X, FILL_MIN_Z, FILL_MAX_X, FILL_MAX_Z, level);
        }
        BlockHitResult hit = SlotRenderer.hitOn(bag.getBlockPos());
        pose.translate(0.5, GHOST_Y, 0.5);
        if (hit != null && bag.isInput(hit)) {
            ItemStack held = SlotRenderer.heldItem();
            ItemStack inSlot = bag.handler().getStackInSlot(0);
            pose.pushPose();
            pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
            SlotRenderer.renderGhostFor(pose, buffers, count == 0 ? ItemStack.EMPTY : inSlot, held, bag.accepts(held));
            pose.popPose();
        }
        if (count > 0 && SlotRenderer.showCounts(bag.getBlockPos())) {
            SlotRenderer.renderCount(pose, buffers, count, COUNT_OFFSET, facing);
        }
        pose.popPose();
    }
}
