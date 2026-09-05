package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreFluids;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.item.HammerItem;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.concurrent.CompletableFuture;

public final class CoreItemTagsProvider extends ItemTagsProvider {

    public CoreItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        for (DeferredItem<HammerItem> hammer : CoreItems.HAMMERS) {
            this.tag(PyrotechTags.Items.HAMMERS).add(hammer.get());
        }
        this.tag(PyrotechTags.Items.STONE_STICKS).add(CoreItems.material(Material.STICK_STONE).get());
        // The stone rocks a stone tool recipe accepts (tool sign-off, item 6).
        this.tag(PyrotechTags.Items.ROCKS).add(
            CoreBlocks.ROCK_STONE.get().asItem(),
            CoreBlocks.ROCK_DIORITE.get().asItem(),
            CoreBlocks.ROCK_GRANITE.get().asItem(),
            CoreBlocks.ROCK_ANDESITE.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE.get().asItem(),
            CoreBlocks.ROCK_LIMESTONE.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE_RED.get().asItem());
        // Ignition fills the igniters and hunting the knives; the files exist from the start so
        // that the tags they feed resolve.
        this.tag(PyrotechTags.Items.IGNITERS);
        this.tag(PyrotechTags.Items.KNIVES);
        this.tag(PyrotechTags.Items.SHARP_TOOLS).addTags(ItemTags.AXES, ItemTags.SWORDS, PyrotechTags.Items.KNIVES);
        for (CoreFluids.Entry fluid : CoreFluids.ALL) {
            this.tag(Tags.Items.BUCKETS).add(fluid.bucket().get());
        }

        // The item halves of the shape tags. #minecraft:wooden_doors stays untouched: that
        // is the tag NeoForge's furnace_fuels data map burns.
        this.copy(BlockTags.SLABS, ItemTags.SLABS);
        this.copy(BlockTags.STAIRS, ItemTags.STAIRS);
        this.copy(BlockTags.WALLS, ItemTags.WALLS);
        this.copy(BlockTags.DOORS, ItemTags.DOORS);
    }
}
