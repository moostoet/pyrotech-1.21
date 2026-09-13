package com.moostoet.pyrotech.datagen.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.HuntingTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * Where the mud spawns (hunting sign-off, item 1): the 1.12 swampland and river, plus the
 * mangrove swamp vanilla lists beside the swamp for surface slimes.
 */
public final class HuntingBiomeTagsProvider extends BiomeTagsProvider {

    public HuntingBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                    ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    public String getName() {
        return "Hunting " + super.getName();
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(HuntingTags.Biomes.MUD_SPAWN_BIOMES).add(Biomes.SWAMP, Biomes.MANGROVE_SWAMP, Biomes.RIVER);
    }
}
