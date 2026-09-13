package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.AnvilBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The item on the anvil, three quarters size. */
public final class AnvilRenderer implements BlockEntityRenderer<AnvilBlockEntity> {

    private static final float SCALE = 0.75f;

    public AnvilRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(AnvilBlockEntity anvil, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = anvil.input().getStackInSlot(0);
        BlockHitResult hit = SlotRenderer.hitOn(anvil.getBlockPos());
        pose.pushPose();
        pose.translate(0.5, 0.75, 0.5);
        pose.scale(SCALE, SCALE, SCALE);
        if (!stack.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, stack, light);
        }
        if (hit != null && hit.getDirection() == Direction.UP) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, stack, held, anvil.input().isItemValid(0, held));
        }
        pose.popPose();
    }
}
