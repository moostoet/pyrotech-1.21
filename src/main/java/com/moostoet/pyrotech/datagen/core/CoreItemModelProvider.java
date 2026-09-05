package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.Material;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Item models the migrated assets lack: one flat model per material item, and one
 * block-parented model per core block item.
 */
public final class CoreItemModelProvider extends ItemModelProvider {

    public CoreItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (Material material : Material.values()) {
            this.singleTexture(material.id(), this.mcLoc("item/generated"), "layer0", this.modLoc("item/" + material.texture()));
        }
        this.blockItem("charcoal_block", "charcoal_block");
        this.blockItem("coal_coke_block", "coal_coke_block");
        this.blockItem("crafting_table_template", "crafting_table_template");
        this.blockItem("refractory_brick_block", "gen/refractory_brick_block/cube_all");
        this.blockItem("masonry_brick_block", "gen/masonry_brick_block/cube_all");
        this.blockItem("limestone", "limestone");
        this.blockItem("fossil_ore", "gen/fossil_ore/cube_all");
        this.blockItem("dense_coal_ore", "gen/dense_coal_ore/cube_all");
        this.blockItem("dense_nether_coal_ore", "gen/dense_nether_coal_ore/cube_all");
        this.blockItem("planks_tarred", "planks_tarred");
        this.blockItem("wool_tarred", "wool_tarred");
        this.blockItem("wood_tar_block", "wood_tar_block");
    }

    private void blockItem(String name, String blockModel) {
        this.withExistingParent(name, this.modLoc("block/" + blockModel));
    }
}
