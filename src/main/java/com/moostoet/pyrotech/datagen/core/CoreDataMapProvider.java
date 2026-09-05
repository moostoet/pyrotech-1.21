package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.ToolLevels;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.DataMapProvider;
import net.neoforged.neoforge.registries.datamaps.builtin.FurnaceFuel;
import net.neoforged.neoforge.registries.datamaps.builtin.NeoForgeDataMaps;

import java.util.concurrent.CompletableFuture;

/**
 * Core's data map entries: the furnace fuels with the 1.12 default ticks (core sign-off,
 * item 2) and the hammer levels (recipe architecture sign-off, item 2).
 */
public final class CoreDataMapProvider extends DataMapProvider {

    public CoreDataMapProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider);
    }

    @Override
    protected void gather(HolderLookup.Provider provider) {
        Builder<FurnaceFuel, net.minecraft.world.item.Item> fuels = this.builder(NeoForgeDataMaps.FURNACE_FUELS);
        fuels.add(CoreItems.material(Material.CHARCOAL_FLAKES), new FurnaceFuel(200), false);
        fuels.add(CoreItems.material(Material.STRAW), new FurnaceFuel(50), false);
        fuels.add(CoreItems.material(Material.COAL_COKE), new FurnaceFuel(3200), false);
        fuels.add(CoreItems.material(Material.COAL_PIECES), new FurnaceFuel(200), false);
        fuels.add(CoreItems.material(Material.BOARD), new FurnaceFuel(75), false);
        fuels.add(CoreItems.material(Material.BOARD_TARRED), new FurnaceFuel(400), false);
        fuels.add(CoreItems.material(Material.KINDLING), new FurnaceFuel(800), false);
        fuels.add(CoreItems.material(Material.KINDLING_TARRED), new FurnaceFuel(1600), false);
        fuels.add(CoreItems.material(Material.PLANT_FIBERS_DRIED), new FurnaceFuel(15), false);
        fuels.add(CoreItems.BURNED_FOOD, new FurnaceFuel(200), false);
        fuels.add(CoreBlocks.COAL_COKE_BLOCK.getId(), new FurnaceFuel(32000), false);
        fuels.add(CoreBlocks.CHARCOAL_BLOCK.getId(), new FurnaceFuel(16000), false);
        fuels.add(CoreBlocks.PLANKS_TARRED.getId(), new FurnaceFuel(800), false);
        fuels.add(CoreBlocks.WOOL_TARRED.getId(), new FurnaceFuel(800), false);
        fuels.add(CoreBlocks.WOOD_TAR_BLOCK.getId(), new FurnaceFuel(8000), false);
        // Still to come with their blocks and items: thatch 200, log pile 3000, rock of wood
        // chips 50, pile of wood chips 400, living tar 32000, pyroberries 400. Tinder is
        // tech/basic's.

        Builder<Integer, net.minecraft.world.item.Item> levels = this.builder(ToolLevels.TOOL_LEVELS);
        levels.add(CoreItems.CRUDE_HAMMER, 0, false);
        levels.add(CoreItems.STONE_HAMMER, 1, false);
        levels.add(CoreItems.BONE_HAMMER, 1, false);
        levels.add(CoreItems.BONE_HAMMER_DURABLE, 1, false);
        levels.add(CoreItems.FLINT_HAMMER, 1, false);
        levels.add(CoreItems.FLINT_HAMMER_DURABLE, 1, false);
        levels.add(CoreItems.GOLD_HAMMER, 1, false);
        levels.add(CoreItems.IRON_HAMMER, 2, false);
        levels.add(CoreItems.OBSIDIAN_HAMMER, 2, false);
        levels.add(CoreItems.DIAMOND_HAMMER, 3, false);
    }
}
