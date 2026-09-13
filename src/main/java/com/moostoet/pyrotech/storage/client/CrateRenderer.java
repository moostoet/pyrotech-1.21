package com.moostoet.pyrotech.storage.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.storage.block.entity.CrateBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** The crate's nine stacks in their cells, a fifth size, seen from the top. */
public final class CrateRenderer implements BlockEntityRenderer<CrateBlockEntity> {

    private static final double THIRD = 1 / 3.0;
    private static final double SIXTH = 1 / 6.0;
    private static final double GAP = 0.025;
    private static final float ITEM_Y = 15 / 16f;
    private static final float SCALE = 0.2f;
    private static final Vec3 COUNT_OFFSET = new Vec3(0, 0.25, 0);

    public CrateRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CrateBlockEntity crate, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        Direction facing = crate.facing();
        BlockHitResult hit = SlotRenderer.hitOn(crate.getBlockPos());
        int hitSlot = hit == null ? -1 : crate.slotAt(hit);
        boolean counts = SlotRenderer.showCounts(crate.getBlockPos());
        ItemStack held = SlotRenderer.heldItem();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, facing);
        for (int slot = 0; slot < crate.handler().getSlots(); slot++) {
            ItemStack stack = crate.handler().getStackInSlot(slot);
            int column = slot % CrateBlockEntity.COLUMNS;
            int row = slot / CrateBlockEntity.COLUMNS;
            pose.pushPose();
            pose.translate(column * (THIRD - GAP) + SIXTH + GAP, ITEM_Y, row * (THIRD - GAP) + SIXTH + GAP);
            pose.pushPose();
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
