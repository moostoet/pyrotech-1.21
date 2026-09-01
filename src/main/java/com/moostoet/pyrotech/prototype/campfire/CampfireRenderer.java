package com.moostoet.pyrotech.prototype.campfire;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * Replaces the athenaeum TESR passes: fuel logs and the cooking item render
 * from the synced block entity inventories, so any wood species keeps its own
 * texture.
 */
public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity> {

    private static final float[][] LOG_OFFSETS = {
        {-0.16f, 0.10f, -0.16f}, {0.16f, 0.10f, -0.16f}, {-0.16f, 0.10f, 0.16f}, {0.16f, 0.10f, 0.16f},
        {-0.16f, 0.34f, -0.16f}, {0.16f, 0.34f, -0.16f}, {-0.16f, 0.34f, 0.16f}, {0.16f, 0.34f, 0.16f},
    };

    @Override
    public void render(CampfireBlockEntity campfire, float partialTick, PoseStack poseStack,
                       MultiBufferSource bufferSource, int packedLight, int packedOverlay) {

        var itemRenderer = Minecraft.getInstance().getItemRenderer();

        for (int i = 0; i < campfire.getFuel().getSlots(); i++) {
            ItemStack log = campfire.getFuel().getStackInSlot(i);
            if (log.isEmpty()) {
                continue;
            }
            poseStack.pushPose();
            poseStack.translate(0.5f + LOG_OFFSETS[i][0], LOG_OFFSETS[i][1], 0.5f + LOG_OFFSETS[i][2]);
            poseStack.mulPose(Axis.YP.rotationDegrees((i >= 4) ? 90 : 0));
            poseStack.scale(0.30f, 0.20f, 0.62f);
            itemRenderer.renderStatic(log, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, campfire.getLevel(), 0);
            poseStack.popPose();
        }

        ItemStack food = campfire.getInput().getStackInSlot(0);
        if (food.isEmpty()) {
            food = campfire.getOutput().getStackInSlot(0);
        }
        if (!food.isEmpty()) {
            poseStack.pushPose();
            poseStack.translate(0.5f, 0.55f, 0.5f);
            poseStack.mulPose(Axis.XP.rotationDegrees(-90));
            poseStack.scale(0.75f, 0.75f, 0.75f);
            itemRenderer.renderStatic(food, ItemDisplayContext.FIXED, packedLight, OverlayTexture.NO_OVERLAY,
                poseStack, bufferSource, campfire.getLevel(), 0);
            poseStack.popPose();
        }
    }
}
