package com.moostoet.pyrotech.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

/** The tags worldgen owns and its datagen writes. */
public final class WorldgenTags {

    private WorldgenTags() {
    }

    public static final class Biomes {

        /** The 1.12 biome lists, translated (worldgen sign-off, item 6). Datapacks extend them. */
        public static final TagKey<Biome> HAS_PYROBERRY_BUSH = tag("has_pyroberry_bush");
        public static final TagKey<Biome> HAS_GLOAMBERRY_BUSH = tag("has_gloamberry_bush");
        public static final TagKey<Biome> HAS_FRECKLEBERRY_PLANT = tag("has_freckleberry_plant");

        private static TagKey<Biome> tag(String name) {
            return TagKey.create(Registries.BIOME, id(name));
        }
    }

    public static final class Blocks {

        /**
         * Rock ground: what the rock patch tests one block below a rock. The 1.12 GROUND,
         * GRASS, and ROCK materials as tags; sand and gravel stay out (sign-off, item 7).
         */
        public static final TagKey<Block> ROCK_PLACEABLE_ON = tag("rock_placeable_on");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, id(name));
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
