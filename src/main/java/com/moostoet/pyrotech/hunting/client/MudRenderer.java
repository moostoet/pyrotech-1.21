package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.entity.MudEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.SlimeModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** The 1.12 {@code RenderMud}: vanilla's slime renderer without the inner layer, on the mud texture, squishing as a slime does. */
public final class MudRenderer extends MobRenderer<MudEntity, SlimeModel<MudEntity>> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "mud"), "main");
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/entity/mud/mud.png");
    private static final float SHADOW_PER_SIZE = 0.25F;

    public MudRenderer(EntityRendererProvider.Context context) {
        super(context, new SlimeModel<>(context.bakeLayer(LAYER)), SHADOW_PER_SIZE);
    }

    @Override
    public void render(MudEntity mud, float yaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        this.shadowRadius = SHADOW_PER_SIZE * mud.getSize();
        super.render(mud, yaw, partialTick, pose, buffers, light);
    }

    @Override
    protected void scale(MudEntity mud, PoseStack pose, float partialTick) {
        pose.scale(0.999F, 0.999F, 0.999F);
        pose.translate(0.0F, 0.001F, 0.0F);
        float size = mud.getSize();
        float squish = Mth.lerp(partialTick, mud.oSquish, mud.squish) / (size * 0.5F + 1.0F);
        float stretch = 1.0F / (squish + 1.0F);
        pose.scale(stretch * size, 1.0F / stretch * size, stretch * size);
    }

    @Override
    public ResourceLocation getTextureLocation(MudEntity mud) {
        return TEXTURE;
    }
}
