package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.ChoppingBlockBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The log on the chopping block, three quarters size. */
public final class ChoppingBlockRenderer implements BlockEntityRenderer<ChoppingBlockBlockEntity> {

    private static final float SCALE = 0.75f;

    public ChoppingBlockRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ChoppingBlockBlockEntity block, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = block.input().getStackInSlot(0);
        BlockHitResult hit = SlotRenderer.hitOn(block.getBlockPos());
        pose.pushPose();
        pose.translate(0.5, 0.75, 0.5);
        pose.scale(SCALE, SCALE, SCALE);
        if (!stack.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, stack, light);
        }
        if (hit != null && hit.getDirection() == Direction.UP) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, stack, held, block.input().isItemValid(0, held));
        }
        pose.popPose();
    }
}
