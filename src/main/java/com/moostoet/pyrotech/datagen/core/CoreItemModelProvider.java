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
        this.blockItem("masonry_brick_slab", "masonry_brick_slab");
        this.blockItem("refractory_brick_slab", "refractory_brick_slab");
        this.blockItem("masonry_brick_stairs", "masonry_brick_stairs");
        this.blockItem("refractory_brick_stairs", "refractory_brick_stairs");
        this.blockItem("masonry_brick_wall", "masonry_brick_wall_inventory");
        this.blockItem("refractory_brick_wall", "refractory_brick_wall_inventory");
        // The glass item shows the fully unconnected face, as it does in hand.
        this.blockItem("refractory_glass", "gen/refractory_glass/connected_0");
        this.blockItem("slag_glass", "gen/slag_glass/connected_0");
        // The two door item models stay static: the migrated assets already parent them on
        // item/generated over the 1.12 door textures, which is what a door item needs.

        // Slice 3. The rocks, the seeds, the berries, the straw bed, and the mulched farmland
        // keep their static item models over the 1.12 item textures.
        this.blockItem("cobblestone_andesite", "gen/cobblestone/cube_all");
        this.blockItem("cobblestone_diorite", "gen/cobblestone/cube_all_2");
        this.blockItem("cobblestone_granite", "gen/cobblestone/cube_all_3");
        this.blockItem("cobblestone_limestone", "gen/cobblestone/cube_all_4");
        this.blockItem("mud", "gen/mud/cube_all");
        this.blockItem("mud_layer", "mud_layer");
        this.blockItem("cob_wet", "gen/cob_wet/cube_all");
        this.blockItem("cob_dry", "gen/cob_dry/cube_all");
        this.blockItem("log_pile", "log_pile");
        this.blockItem("pile_ash", "gen/pile_ash/cube_all");
        this.blockItem("pile_wood_chips", "gen/pile_wood_chips/cube_all");
        this.blockItem("dense_quartz_ore_large", "gen/dense_quartz_ore_large/dense_crystal_ore_large_a");
        this.blockItem("dense_quartz_ore_small", "gen/dense_quartz_ore_small/dense_crystal_ore_small_a");
        this.blockItem("dense_quartz_ore_rocks", "gen/dense_quartz_ore_rocks/rock_small_a");
        this.blockItem("dense_redstone_ore_large", "gen/dense_redstone_ore_large/dense_crystal_ore_large_a");
        this.blockItem("dense_redstone_ore_small", "gen/dense_redstone_ore_small/dense_crystal_ore_small_a");
        this.blockItem("dense_redstone_ore_rocks", "gen/dense_redstone_ore_rocks/rock_small_a");
        this.blockItem("living_tar", "living_tar");
        this.blockItem("thatch", "thatch");
    }

    private void blockItem(String name, String blockModel) {
        this.withExistingParent(name, this.modLoc("block/" + blockModel));
    }
}
