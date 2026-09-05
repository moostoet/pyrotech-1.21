package com.moostoet.pyrotech.datagen.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class CoreBlockTagsProvider extends BlockTagsProvider {

    public CoreBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                 ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // The 1.12 harvest tools and levels.
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE).add(
            CoreBlocks.CHARCOAL_BLOCK.get(),
            CoreBlocks.COAL_COKE_BLOCK.get(),
            CoreBlocks.CRAFTING_TABLE_TEMPLATE.get(),
            CoreBlocks.REFRACTORY_BRICK_BLOCK.get(),
            CoreBlocks.MASONRY_BRICK_BLOCK.get(),
            CoreBlocks.LIMESTONE.get(),
            CoreBlocks.FOSSIL_ORE.get(),
            CoreBlocks.DENSE_COAL_ORE.get(),
            CoreBlocks.DENSE_NETHER_COAL_ORE.get());
        this.tag(BlockTags.MINEABLE_WITH_AXE).add(CoreBlocks.PLANKS_TARRED.get());
        this.tag(BlockTags.MINEABLE_WITH_SHOVEL).add(CoreBlocks.WOOD_TAR_BLOCK.get());
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(CoreBlocks.LIMESTONE.get());
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(CoreBlocks.DENSE_COAL_ORE.get());
        this.tag(BlockTags.NEEDS_DIAMOND_TOOL).add(CoreBlocks.DENSE_NETHER_COAL_ORE.get());

        this.tag(PyrotechTags.Blocks.REFRACTORY).add(CoreBlocks.REFRACTORY_BRICK_BLOCK.get());
    }
}
