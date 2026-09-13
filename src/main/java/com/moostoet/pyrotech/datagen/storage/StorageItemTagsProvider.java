package com.moostoet.pyrotech.datagen.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.storage.StorageTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * The two bag tags with the 1.12 whitelists as direct entries (storage sign-off, item 2):
 * the rock bag takes the eight listed rock kinds plus the grass and netherrack rocks, and
 * the durable bag adds dirt, cobblestone, gravel, both sandstones, core's four cobblestones,
 * and cobbled deepslate. The sand, red sand, mud, and wood chip rocks stay out, as in 1.12.
 */
public final class StorageItemTagsProvider extends ItemTagsProvider {

    public StorageItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                   CompletableFuture<TagLookup<Block>> blockTags, ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Storage " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(StorageTags.Items.ROCK_BAG_ITEMS).add(
            CoreBlocks.ROCK_STONE.get().asItem(),
            CoreBlocks.ROCK_GRANITE.get().asItem(),
            CoreBlocks.ROCK_DIORITE.get().asItem(),
            CoreBlocks.ROCK_ANDESITE.get().asItem(),
            CoreBlocks.ROCK_DIRT.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE.get().asItem(),
            CoreBlocks.ROCK_LIMESTONE.get().asItem(),
            CoreBlocks.ROCK_SANDSTONE_RED.get().asItem(),
            CoreBlocks.ROCK_GRASS.get().asItem(),
            CoreBlocks.ROCK_NETHERRACK.get().asItem());
        this.tag(StorageTags.Items.DURABLE_ROCK_BAG_ITEMS)
            .addTag(StorageTags.Items.ROCK_BAG_ITEMS)
            .add(Items.DIRT,
                Items.COBBLESTONE,
                Items.GRAVEL,
                Items.SANDSTONE,
                Items.RED_SANDSTONE,
                CoreBlocks.COBBLESTONE_ANDESITE.get().asItem(),
                CoreBlocks.COBBLESTONE_DIORITE.get().asItem(),
                CoreBlocks.COBBLESTONE_GRANITE.get().asItem(),
                CoreBlocks.COBBLESTONE_LIMESTONE.get().asItem(),
                Items.COBBLED_DEEPSLATE);
    }
}
