package com.moostoet.pyrotech.tech.basic.client;

import com.moostoet.pyrotech.library.interaction.client.SlotRenderer;
import com.moostoet.pyrotech.tech.basic.block.entity.CampfireBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;

/** The campfire's logs, four leaning on the fire and four laid across them, and the food over it. */
public final class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity> {

    private static final float FOOD_SCALE = 0.75f;

    public CampfireRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(CampfireBlockEntity campfire, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        for (int slot = 0; slot < CampfireBlockEntity.FUEL_SLOTS; slot++) {
            ItemStack log = campfire.fuel().getStackInSlot(slot);
            if (log.isEmpty()) {
                continue;
            }
            pose.pushPose();
            if (slot < 4) {
                pose.translate(0.5, 0.20, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(90 * slot));
                pose.translate(0.375, 0, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(67.5f));
            } else {
                pose.translate(0.5, 0.125, 0.5);
                pose.mulPose(Axis.YP.rotationDegrees(90 * (slot % 4) + 45));
                pose.translate(0.4375, 0, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(90));
            }
            pose.scale(4 / 16f, 8 / 16f, 4 / 16f);
            SlotRenderer.renderItem(pose, buffers, log, light);
            pose.popPose();
        }
        ItemStack output = campfire.output().getStackInSlot(0);
        ItemStack food = output.isEmpty() ? campfire.input().getStackInSlot(0) : output;
        BlockHitResult hit = SlotRenderer.hitOn(campfire.getBlockPos());
        boolean hover = hit != null && hit.getDirection() == Direction.UP && !campfire.isDead();
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        pose.scale(FOOD_SCALE, FOOD_SCALE, FOOD_SCALE);
        if (!food.isEmpty()) {
            SlotRenderer.renderItem(pose, buffers, food, light);
        }
        if (hover) {
            ItemStack held = SlotRenderer.heldItem();
            SlotRenderer.renderGhostFor(pose, buffers, food, held, campfire.canCook(held));
        }
        pose.popPose();
    }
}
