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
 *
 * <p>The log layout copies the 1.12 {@code CampfireInteractionLogRenderer}.
 * The first four logs lean into the middle from north, east, south and west.
 * The next four lie flat on the diagonals. Each log is the wood's own block
 * model drawn as a 4x8x4 pixel column, so the ring texture sits on the ends.
 */
public class CampfireRenderer implements BlockEntityRenderer<CampfireBlockEntity> {

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
            positionLog(poseStack, i);
            // NONE skips the item's display transform, so the block model is a
            // unit cube centred on the origin and the scale below is exact.
            itemRenderer.renderStatic(log, ItemDisplayContext.NONE, packedLight, OverlayTexture.NO_OVERLAY,
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

    /**
     * Same transforms as the 1.12 renderer. The log's long axis is Y before
     * the tilt, which keeps a vanilla log's rings on the two ends.
     */
    private static void positionLog(PoseStack poseStack, int slot) {
        if (slot < 4) {
            // Leaning logs: one per side, tilted 67.5 degrees toward the middle.
            poseStack.translate(0.5f, 0.20f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(90 * slot));
            poseStack.translate(0.375f, 0, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(67.5f));
        } else {
            // Flat logs: one per diagonal, lying on the ground.
            poseStack.translate(0.5f, 0.125f, 0.5f);
            poseStack.mulPose(Axis.YP.rotationDegrees(90 * (slot % 4) + 45));
            poseStack.translate(0.4375f, 0, 0);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90));
        }
        poseStack.scale(4 / 16f, 8 / 16f, 4 / 16f);
    }
}
