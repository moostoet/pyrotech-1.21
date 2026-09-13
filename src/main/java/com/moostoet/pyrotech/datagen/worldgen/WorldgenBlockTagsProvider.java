package com.moostoet.pyrotech.datagen.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.worldgen.WorldgenTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

public final class WorldgenBlockTagsProvider extends BlockTagsProvider {

    public WorldgenBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Worldgen " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(WorldgenTags.Blocks.ROCK_PLACEABLE_ON).addTags(
            BlockTags.DIRT,
            BlockTags.BASE_STONE_OVERWORLD,
            BlockTags.TERRACOTTA);
    }
}
