package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.hunting.HuntingBlocks;
import com.moostoet.pyrotech.hunting.block.entity.ButchersBlockBlockEntity;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;

/** The carcass on the butcher's block at the 1.12 transform: centred, 1.375 up, at three quarters, with the ghost of a held one. */
public final class ButchersBlockRenderer implements BlockEntityRenderer<ButchersBlockBlockEntity> {

    private static final float ITEM_Y = 1.375f;
    private static final float SCALE = 0.75f;

    public ButchersBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ButchersBlockBlockEntity block, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack carcass = block.carcass();
        BlockHitResult hit = SlotRenderer.hitOn(block.getBlockPos());
        pose.pushPose();
        pose.translate(0.5, ITEM_Y, 0.5);
        pose.scale(SCALE, SCALE, SCALE);
        if (!carcass.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, carcass, light);
        }
        if (hit != null && hit.getDirection() == Direction.UP) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, carcass, held, held.is(HuntingBlocks.CARCASS_ITEM.get()));
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(ButchersBlockBlockEntity block) {
        return new AABB(block.getBlockPos()).expandTowards(0, 0.75, 0);
    }
}
