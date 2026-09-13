package com.moostoet.pyrotech.hunting.client;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.entity.PyrotechArrowEntity;
import net.minecraft.client.renderer.entity.ArrowRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

/** Vanilla's arrow renderer over the migrated flint and bone arrow textures. */
public final class PyrotechArrowRenderer extends ArrowRenderer<PyrotechArrowEntity> {

    private static final ResourceLocation FLINT = texture("arrow_flint");
    private static final ResourceLocation BONE = texture("arrow_bone");

    public PyrotechArrowRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(PyrotechArrowEntity arrow) {
        return arrow.kind() == PyrotechArrowEntity.Kind.BONE ? BONE : FLINT;
    }

    private static ResourceLocation texture(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, "textures/entity/projectiles/" + name + ".png");
    }
}
