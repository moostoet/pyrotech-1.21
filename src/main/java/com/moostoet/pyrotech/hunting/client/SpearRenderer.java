package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.hunting.entity.SpearEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;

/** The 1.12 {@code RenderSpear}: the spear's own item, turned to its flight and tilted along its diagonal. */
public final class SpearRenderer extends EntityRenderer<SpearEntity> {

    private final ItemRenderer itemRenderer;

    public SpearRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SpearEntity spear, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(yaw));
        pose.mulPose(Axis.XP.rotationDegrees(-Mth.lerp(partialTick, spear.xRotO, spear.getXRot())));
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.mulPose(Axis.ZP.rotationDegrees(-45));
        this.itemRenderer.renderStatic(spear.getItem(), ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
            spear.level(), spear.getId());
        pose.popPose();
        super.render(spear, yaw, partialTick, pose, buffers, light);
    }

    @Override
    public ResourceLocation getTextureLocation(SpearEntity spear) {
        return InventoryMenu.BLOCK_ATLAS;
    }
}
