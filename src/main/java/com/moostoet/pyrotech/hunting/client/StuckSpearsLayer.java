package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.hunting.entity.StuckSpears;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * The 1.12 {@code LayerSpear}: every spear stuck in an entity drawn at a point on its body
 * chosen by a random seeded per entity, at the 1.12 tilt. The point comes from the
 * entity's bounds rather than a model box, which every model has.
 */
public final class StuckSpearsLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final float MODEL_FEET_Y = 1.501f;

    private final ItemRenderer itemRenderer;

    public StuckSpearsLayer(RenderLayerParent<T, M> parent, ItemRenderer itemRenderer) {
        super(parent);
        this.itemRenderer = itemRenderer;
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        StuckSpears spears = StuckSpears.of(entity);
        if (spears.isEmpty()) {
            return;
        }
        RandomSource random = RandomSource.create(entity.getId());
        float width = entity.getBbWidth();
        float height = entity.getBbHeight();
        for (ItemStack spear : spears.spears()) {
            float x = (random.nextFloat() - 0.5f) * width;
            float y = MODEL_FEET_Y - random.nextFloat() * height;
            float z = (random.nextFloat() - 0.5f) * width;
            pose.pushPose();
            pose.translate(x, y, z);
            pose.mulPose(Axis.YP.rotationDegrees(random.nextFloat() * 360));
            pose.mulPose(Axis.XP.rotationDegrees(-45));
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            pose.mulPose(Axis.ZP.rotationDegrees(-45));
            this.itemRenderer.renderStatic(spear, ItemDisplayContext.NONE, light, OverlayTexture.NO_OVERLAY, pose, buffers,
                entity.level(), entity.getId());
            pose.popPose();
        }
    }
}
