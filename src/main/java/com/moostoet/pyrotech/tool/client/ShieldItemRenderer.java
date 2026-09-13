package com.moostoet.pyrotech.tool.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a shield item from its layer and texture, the 1.12 {@code TEISRShield} as a
 * {@link BlockEntityWithoutLevelRenderer}. Vanilla's shield flips the model the same way.
 */
public final class ShieldItemRenderer extends BlockEntityWithoutLevelRenderer {

    private final ModelLayerLocation layer;
    private final ResourceLocation texture;
    @Nullable
    private PyrotechShieldModel model;

    private ShieldItemRenderer(ModelLayerLocation layer, ResourceLocation texture) {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
        this.layer = layer;
        this.texture = texture;
    }

    /** The client extension that hands an item this renderer, built on first use. */
    public static IClientItemExtensions extensions(ModelLayerLocation layer, ResourceLocation texture) {
        return new IClientItemExtensions() {
            @Nullable
            private ShieldItemRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (this.renderer == null) {
                    this.renderer = new ShieldItemRenderer(layer, texture);
                }
                return this.renderer;
            }
        };
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        this.model = null;
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext displayContext, PoseStack poseStack, MultiBufferSource buffer,
                             int packedLight, int packedOverlay) {
        if (this.model == null) {
            this.model = new PyrotechShieldModel(Minecraft.getInstance().getEntityModels().bakeLayer(this.layer));
        }
        poseStack.pushPose();
        poseStack.scale(1.0f, -1.0f, -1.0f);
        VertexConsumer consumer = ItemRenderer.getFoilBufferDirect(buffer, this.model.renderType(this.texture), true, stack.hasFoil());
        this.model.renderToBuffer(poseStack, consumer, packedLight, packedOverlay);
        poseStack.popPose();
    }
}
