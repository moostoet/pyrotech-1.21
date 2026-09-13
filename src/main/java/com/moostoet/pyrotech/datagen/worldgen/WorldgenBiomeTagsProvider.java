package com.moostoet.pyrotech.datagen.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.worldgen.WorldgenTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.concurrent.CompletableFuture;

/**
 * The 1.12 biome lists as 1.21 ids (worldgen sign-off, item 6). The hills and mutated
 * variants fold into the base biome that survived, gloamberry keeps dark forest out
 * because 1.12 listed only its mutated variant, and no biome added since 1.12 joins.
 */
public final class WorldgenBiomeTagsProvider extends BiomeTagsProvider {

    public WorldgenBiomeTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                     ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, Pyrotech.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        this.tag(WorldgenTags.Biomes.HAS_PYROBERRY_BUSH).add(Biomes.DESERT);
        this.tag(WorldgenTags.Biomes.HAS_GLOAMBERRY_BUSH).add(
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA);
        this.tag(WorldgenTags.Biomes.HAS_FRECKLEBERRY_PLANT).add(
            Biomes.PLAINS,
            Biomes.SUNFLOWER_PLAINS,
            Biomes.FOREST,
            Biomes.FLOWER_FOREST,
            Biomes.BIRCH_FOREST,
            Biomes.OLD_GROWTH_BIRCH_FOREST,
            Biomes.DARK_FOREST,
            Biomes.TAIGA,
            Biomes.OLD_GROWTH_PINE_TAIGA,
            Biomes.OLD_GROWTH_SPRUCE_TAIGA,
            Biomes.SAVANNA,
            Biomes.SAVANNA_PLATEAU,
            Biomes.WINDSWEPT_SAVANNA);
    }
}
