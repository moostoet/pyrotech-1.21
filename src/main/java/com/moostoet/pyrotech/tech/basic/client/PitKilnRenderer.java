package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.PitKilnVariant;
import com.moostoet.pyrotech.tech.basic.block.entity.PitKilnBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The ware in an open pit, and the three logs across a thatched one. */
public final class PitKilnRenderer implements BlockEntityRenderer<PitKilnBlockEntity> {

    private static final double THIRD = 1 / 3.0;
    private static final double SIXTH = 1 / 6.0;
    private static final float WARE_SCALE = 0.5f;

    public PitKilnRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PitKilnBlockEntity kiln, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        PitKilnVariant variant = kiln.variant();
        BlockHitResult hit = SlotRenderer.hitOn(kiln.getBlockPos());
        ItemStack held = SlotRenderer.heldItem();
        if (variant == PitKilnVariant.EMPTY) {
            ItemStack ware = kiln.input().getStackInSlot(0);
            if (ware.isEmpty()) {
                for (int slot = 0; slot < PitKilnBlockEntity.OUTPUT_SLOTS && ware.isEmpty(); slot++) {
                    ware = kiln.output().getStackInSlot(slot);
                }
            }
            pose.pushPose();
            pose.translate(0.5, 0.4, 0.5);
            pose.scale(WARE_SCALE, WARE_SCALE, WARE_SCALE);
            if (!ware.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, ware, light);
            }
            if (hit != null) {
                SlotRenderer.renderGhostFor(pose, buffers, ware, held, kiln.input().isItemValid(0, held));
            }
            pose.popPose();
            return;
        }
        if (variant != PitKilnVariant.THATCH && variant != PitKilnVariant.WOOD) {
            return;
        }
        int hitSlot = hit == null ? -1 : kiln.logSlotAt(hit);
        for (int slot = 0; slot < PitKilnBlockEntity.LOG_SLOTS; slot++) {
            ItemStack log = kiln.logs().getStackInSlot(slot);
            pose.pushPose();
            pose.translate(slot * THIRD + SIXTH, 2 * THIRD + SIXTH, 0.5);
            pose.mulPose(Axis.XP.rotationDegrees(90));
            pose.scale((float) THIRD, 1, (float) THIRD);
            if (!log.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, log, light);
            }
            if (slot == hitSlot) {
                SlotRenderer.renderGhostFor(pose, buffers, log, held, kiln.logs().isItemValid(slot, held));
            }
            pose.popPose();
        }
    }
}
