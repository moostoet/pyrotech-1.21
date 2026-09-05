package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/** The tags core owns. Other modules read them; core's datagen writes them. */
public final class PyrotechTags {

    private PyrotechTags() {
    }

    public static final class Items {

        /** Every item that counts as a hammer on the worktable, the anvil, and a bloom. */
        public static final TagKey<Item> HAMMERS = tag("hammers");
        /** The items that light Pyrotech blocks. Ignition fills it; vanilla fire starters stay out. */
        public static final TagKey<Item> IGNITERS = tag("igniters");
        /** The stone rocks: the ones a stone tool recipe accepts. */
        public static final TagKey<Item> ROCKS = tag("rocks");
        public static final TagKey<Item> STONE_STICKS = tag("stone_sticks");
        /** Hunting's knives. Declared here because {@link #SHARP_TOOLS} includes it. */
        public static final TagKey<Item> KNIVES = tag("knives");
        /** Any axe, sword, or knife: the 1.12 {@code toolSharp} ore dictionary name. */
        public static final TagKey<Item> SHARP_TOOLS = tag("sharp_tools");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, id(name));
        }
    }

    public static final class Blocks {

        /** A wall of a refractory burn. Refractory brick, glass, the double slab, and datapack additions. */
        public static final TagKey<Block> REFRACTORY = tag("refractory");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, id(name));
        }
    }

    public static final class Fluids {

        /** A fluid that douses a lit torch or campfire. Water. */
        public static final TagKey<Fluid> DOUSING = tag("dousing");

        private static TagKey<Fluid> tag(String name) {
            return TagKey.create(Registries.FLUID, id(name));
        }
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
