package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.StashBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The stash's stack standing on its lid at three quarters, its count above it. */
public final class StashRenderer implements BlockEntityRenderer<StashBlockEntity> {

    private static final float ITEM_Y = 7 / 16f;
    private static final float SCALE = 0.75f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.5, 0);

    public StashRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(StashBlockEntity stash, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = stash.facing();
        ItemStack stack = stash.handler().getStackInSlot(0);
        BlockHitResult hit = SlotRenderer.hitOn(stash.getBlockPos());
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        pose.translate(0.5, ITEM_Y, 0.5);
        pose.pushPose();
        pose.scale(SCALE, SCALE, SCALE);
        if (!stack.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, stack, light);
        }
        if (hit != null && stash.slotAt(hit) == 0) {
            SlotRenderer.renderGhostFor(pose, buffers, stack, SlotRenderer.heldItem(), true);
        }
        pose.popPose();
        if (!stack.isEmpty() && SlotRenderer.showCounts(stash.getBlockPos())) {
            SlotRenderer.renderCount(pose, buffers, stack.getCount(), COUNT_OFFSET, facing);
        }
        pose.popPose();
    }
}
