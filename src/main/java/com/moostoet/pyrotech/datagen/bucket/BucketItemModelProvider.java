package com.moostoet.pyrotech.datagen.bucket;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.BucketItems;
import com.moostoet.pyrotech.bucket.item.PyrotechBucketItem;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

/**
 * The four bucket item models the migration could not convert: the 1.12 files were
 * {@code forge_marker} blockstates on {@code forge:forgebucket}. Each is NeoForge's
 * {@code fluid_container} model over the tier's textures, with the base as the fluid mask
 * so the fluid fills the bucket's silhouette, and the cover drawn as it is over that, as
 * the 1.12 model drew it. The two unfired buckets keep their static models.
 */
public final class BucketItemModelProvider extends ItemModelProvider {

    public BucketItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Bucket Item Models: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerModels() {
        for (DeferredItem<PyrotechBucketItem> bucket : BucketItems.BUCKETS) {
            String texture = "item/" + bucket.get().tier().id() + "_bucket";
            this.getBuilder(bucket.getId().getPath())
                .parent(new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket")))
                .texture("base", this.modLoc(texture + "_base"))
                .texture("fluid", this.modLoc(texture + "_base"))
                .texture("cover", this.modLoc(texture + "_cover"))
                .customLoader(DynamicFluidContainerModelBuilder::begin)
                .fluid(Fluids.EMPTY)
                .flipGas(true)
                .coverIsMask(false);
        }
    }
}
