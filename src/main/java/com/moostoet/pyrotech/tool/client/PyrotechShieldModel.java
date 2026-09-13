package com.moostoet.pyrotech.tool.client;

import com.moostoet.pyrotech.Pyrotech;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/** The two 1.12 shield models, a plate and a handle each, as layer definitions. */
public final class PyrotechShieldModel extends Model {

    public static final ModelLayerLocation CRUDE_LAYER = layer("crude_shield");
    public static final ModelLayerLocation DURABLE_LAYER = layer("durable_shield");

    private final ModelPart root;

    public PyrotechShieldModel(ModelPart root) {
        super(RenderType::entityCutoutNoCull);
        this.root = root;
    }

    /** The 1.12 {@code ModelCrudeShield}: a ten by ten plate on a 32 by 32 texture. */
    public static LayerDefinition createCrudeLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(-5, -5, -2, 10, 10, 1), PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(0, 11).addBox(-1, -3, -1, 2, 6, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 32, 32);
    }

    /** The 1.12 {@code ModelDurableShield}: a twelve by twenty-one plate on a 64 by 64 texture. */
    public static LayerDefinition createDurableLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("plate", CubeListBuilder.create().texOffs(0, 0).addBox(-6, -11, -2, 12, 21, 1), PartPose.ZERO);
        root.addOrReplaceChild("handle", CubeListBuilder.create().texOffs(26, 0).addBox(-1, -3, -1, 2, 6, 6), PartPose.ZERO);
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, buffer, packedLight, packedOverlay, color);
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name), "main");
    }
}
