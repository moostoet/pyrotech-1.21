package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.CompostBinBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The compost bin draws nothing of its own; it only ghosts a compostable held item over the top. */
public final class CompostBinRenderer implements BlockEntityRenderer<CompostBinBlockEntity> {

    private static final float GHOST_SCALE = 0.75f;

    public CompostBinRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CompostBinBlockEntity bin, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockHitResult hit = SlotRenderer.hitOn(bin.getBlockPos());
        if (hit == null || hit.getDirection() != Direction.UP) {
            return;
        }
        ItemStack held = SlotRenderer.heldItem();
        if (held.isEmpty() || bin.input().insertItem(0, held, true).getCount() == held.getCount()) {
            return;
        }
        pose.pushPose();
        pose.translate(0.5, 1, 0.5);
        pose.scale(GHOST_SCALE, GHOST_SCALE, GHOST_SCALE);
        SlotRenderer.renderGhost(pose, buffers, held);
        pose.popPose();
    }
}
