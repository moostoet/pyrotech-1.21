package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.DryingRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The crude rack's one item hung against its wall; the frame's four laid flat in their cells. */
public final class DryingRackRenderer implements BlockEntityRenderer<DryingRackBlockEntity> {

    private static final float CRUDE_SCALE = 0.75f;
    private static final float SCALE = 0.25f;

    public DryingRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(DryingRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockHitResult hit = SlotRenderer.hitOn(rack.getBlockPos());
        int hitSlot = hit == null ? -1 : rack.slotAt(hit);
        ItemStack held = SlotRenderer.heldItem();
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, rack.facing());
        for (int slot = 0; slot < rack.slots(); slot++) {
            ItemStack output = rack.output().getStackInSlot(slot);
            ItemStack shown = output.isEmpty() ? rack.input().getStackInSlot(slot) : output;
            pose.pushPose();
            if (rack.isCrude()) {
                pose.translate(0.5, 0.5, 0.15);
                pose.scale(CRUDE_SCALE, CRUDE_SCALE, CRUDE_SCALE);
            } else {
                int x = slot % 2;
                int z = slot / 2;
                pose.translate(x * 0.375 + 0.25 + 0.0625, 0.75 + 0.03125, z * 0.375 + 0.25 + 0.0625);
                pose.mulPose(Axis.XP.rotationDegrees(-90));
                pose.scale(SCALE, SCALE, SCALE);
            }
            if (!shown.isEmpty()) {
                SlotRenderer.renderItem(pose, buffers, shown, light);
            }
            if (slot == hitSlot) {
                SlotRenderer.renderGhostFor(pose, buffers, shown, held, true);
            }
            pose.popPose();
        }
        pose.popPose();
    }
}
