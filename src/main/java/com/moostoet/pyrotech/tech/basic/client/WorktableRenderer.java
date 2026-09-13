package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.WorktableBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The nine grid items flat on the table top and the three shelf items under its front edge. */
public final class WorktableRenderer implements BlockEntityRenderer<WorktableBlockEntity> {

    private static final double THIRD = 1 / 3.0;
    private static final double SIXTH = 1 / 6.0;
    private static final double GAP = 0.025;
    private static final double GRID_Y = 15 / 16.0 + 1 / 32.0;
    private static final double SHELF_Y = 5 / 16.0;
    private static final float SCALE = 0.2f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.25, 0);

    public WorktableRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(WorktableBlockEntity table, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = table.facing();
        BlockHitResult hit = SlotRenderer.hitOn(table.getBlockPos());
        int hitSlot = hit == null || hit.getDirection() != Direction.UP ? -1 : table.slotAt(hit);
        boolean counts = SlotRenderer.showCounts(table.getBlockPos());
        ItemStack held = SlotRenderer.heldItem();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        for (int slot = 0; slot < WorktableBlockEntity.GRID_SLOTS; slot++) {
            int x = WorktableBlockEntity.GRID_SIZE - 1 - slot % WorktableBlockEntity.GRID_SIZE;
            int z = WorktableBlockEntity.GRID_SIZE - 1 - slot / WorktableBlockEntity.GRID_SIZE;
            this.renderSlot(pose, buffers, light, facing, table.grid().getStackInSlot(slot),
                x * (THIRD - GAP) + SIXTH + GAP, GRID_Y, z * (THIRD - GAP) + SIXTH + GAP,
                slot == hitSlot, held, !held.is(PyrotechTags.Items.HAMMERS), counts);
        }
        for (int slot = 0; slot < WorktableBlockEntity.SHELF_SLOTS; slot++) {
            this.renderSlot(pose, buffers, light, facing, table.shelf().getStackInSlot(slot),
                slot * (THIRD - GAP) + SIXTH + GAP, SHELF_Y, SIXTH + GAP,
                WorktableBlockEntity.GRID_SLOTS + slot == hitSlot, held, true, counts);
        }
        pose.popPose();
    }

    private void renderSlot(PoseStack pose, MultiBufferSource buffers, int light, Direction facing, ItemStack stack,
                            double x, double y, double z, boolean hovered, ItemStack held, boolean heldValid, boolean counts) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.mulPose(Axis.XP.rotationDegrees(-90));
        pose.scale(SCALE, SCALE, SCALE);
        if (!stack.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, stack, light);
        }
        if (hovered) {
            SlotRenderer.renderGhostFor(pose, buffers, stack, held, heldValid);
        }
        pose.popPose();
        if (counts && !stack.isEmpty()) {
            SlotRenderer.renderCount(pose, buffers, stack.getCount(), COUNT_OFFSET, facing);
        }
        pose.popPose();
    }
}
