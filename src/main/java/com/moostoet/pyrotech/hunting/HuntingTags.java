package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.material.Fluid;

/** The tags hunting owns; hunting's datagen writes them. */
public final class HuntingTags {

    private HuntingTags() {
    }

    public static final class Items {

        /** The seven hunter's knives. With the butcher's knives it fills core's {@code #pyrotech:knives}. */
        public static final TagKey<Item> HUNTERS_KNIVES = tag("hunters_knives");
        public static final TagKey<Item> BUTCHERS_KNIVES = tag("butchers_knives");
        /** The pelts a knife scrapes into a scraped hide: the 1.12 {@code hideScrapeable}. */
        public static final TagKey<Item> SCRAPEABLE_HIDES = tag("scrapeable_hides");
        /** The bat pelt and the rabbit hide, which scrape into a small scraped hide. */
        public static final TagKey<Item> SMALL_SCRAPEABLE_HIDES = tag("small_scrapeable_hides");
        public static final TagKey<Item> LEATHER_REPAIR_KITS = tag("leather_repair_kits");
        /** The drops a carcass captures from a kill: the 1.12 {@code DROP_CAPTURE_LIST} (hunting sign-off, item 2). */
        public static final TagKey<Item> CARCASS_CAPTURED = tag("carcass_captured");
        /** Core's eight shards: the 1.12 {@code shard} ore dictionary name, read by the shard scraping recipes. */
        public static final TagKey<Item> SHARDS = tag("shards");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, id(name));
        }
    }

    public static final class Fluids {

        /** Tannin, source and flowing; tech/basic's soaking pot and barrel recipes match on it. */
        public static final TagKey<Fluid> TANNIN = tag("tannin");

        private static TagKey<Fluid> tag(String name) {
            return TagKey.create(Registries.FLUID, id(name));
        }
    }

    public static final class Biomes {

        /** Where the animated mud spawns and where its spawn rule passes (hunting sign-off, item 1). */
        public static final TagKey<Biome> MUD_SPAWN_BIOMES = tag("mud_spawn_biomes");

        private static TagKey<Biome> tag(String name) {
            return TagKey.create(Registries.BIOME, id(name));
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
