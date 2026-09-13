package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.ShelfBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The shelf's nine stacks in their cells, a fifth size, facing out of the front. */
public final class ShelfRenderer implements BlockEntityRenderer<ShelfBlockEntity> {

    private static final double THIRD = 1 / 3.0;
    private static final double SIXTH = 1 / 6.0;
    private static final double GAP = 0.025;
    private static final double BACK_Z = 2 * THIRD + SIXTH - GAP;
    private static final double FORWARD_Z = SIXTH - GAP;
    private static final float SCALE = 0.2f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.125, 0);

    public ShelfRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ShelfBlockEntity shelf, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = shelf.facing();
        double z = shelf.isForward() ? FORWARD_Z : BACK_Z;
        BlockHitResult hit = SlotRenderer.hitOn(shelf.getBlockPos());
        int hitSlot = hit == null ? -1 : shelf.slotAt(hit);
        boolean counts = SlotRenderer.showCounts(shelf.getBlockPos());
        ItemStack held = SlotRenderer.heldItem();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        for (int slot = 0; slot < shelf.handler().getSlots(); slot++) {
            ItemStack stack = shelf.handler().getStackInSlot(slot);
            int column = slot % ShelfBlockEntity.COLUMNS;
            int row = slot / ShelfBlockEntity.COLUMNS;
            pose.pushPose();
            pose.translate(column * (THIRD - GAP) + SIXTH + GAP, row * (THIRD - GAP) + SIXTH, z);
            pose.pushPose();
            pose.mulPose(Axis.YP.rotationDegrees(180));
            pose.scale(SCALE, SCALE, SCALE);
            if (!stack.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, stack, light);
            }
            if (slot == hitSlot) {
                SlotRenderer.renderGhostFor(pose, buffers, stack, held, true);
            }
            pose.popPose();
            if (counts && !stack.isEmpty()) {
                SlotRenderer.renderCount(pose, buffers, stack.getCount(), COUNT_OFFSET, facing);
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
