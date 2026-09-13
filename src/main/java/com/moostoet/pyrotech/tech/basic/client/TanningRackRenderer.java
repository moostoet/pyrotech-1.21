package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.TanningRackBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The hide on the frame, leaning back a little, turned with the rack. */
public final class TanningRackRenderer implements BlockEntityRenderer<TanningRackBlockEntity> {

    private static final float SCALE = 0.75f;

    public TanningRackRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TanningRackBlockEntity rack, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack shown = rack.displayed();
        BlockHitResult hit = SlotRenderer.hitOn(rack.getBlockPos());
        pose.pushPose();
        SlotRenderer.rotateToFacing(pose, rack.facing());
        pose.translate(0.5, 0.525, 0.475);
        pose.mulPose(Axis.XP.rotationDegrees(22.5f));
        pose.scale(SCALE, SCALE, SCALE);
        if (!shown.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, shown, light);
        }
        if (hit != null) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, shown, held,
                rack.output().getStackInSlot(0).isEmpty() && rack.input().isItemValid(0, held));
        }
        pose.popPose();
    }
}
