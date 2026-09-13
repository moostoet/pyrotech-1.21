package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingFluids;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.client.model.generators.loaders.DynamicFluidContainerModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * The four hunting item models the migration could not derive: the two block items on
 * their block models, the spawn egg on vanilla's template, and the tannin bucket as a
 * fluid container. The 59 migrated hunting item models stay static.
 */
public final class HuntingItemModelProvider extends ItemModelProvider {

    public HuntingItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Hunting Item Models: " + Pyrotech.MOD_ID;
    }

    @Override
    protected void registerModels() {
        this.withExistingParent("carcass", this.modLoc("block/carcass"));
        this.withExistingParent("butchers_block", this.modLoc("block/butchers_block"));
        this.withExistingParent("mud_spawn_egg", this.mcLoc("item/template_spawn_egg"));
        this.getBuilder(HuntingFluids.TANNIN.bucket().getId().getPath())
            .parent(new ModelFile.UncheckedModelFile(ResourceLocation.fromNamespaceAndPath("neoforge", "item/bucket")))
            .customLoader(DynamicFluidContainerModelBuilder::begin)
            .fluid(HuntingFluids.TANNIN.source().get());
    }
}
