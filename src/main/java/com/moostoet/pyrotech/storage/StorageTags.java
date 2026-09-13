package com.moostoet.pyrotech.storage;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The tags storage owns; storage's datagen writes them. */
public final class StorageTags {

    private StorageTags() {
    }

    public static final class Items {

        /** What the rock bag carries: the 1.12 whitelist as a tag (storage sign-off, item 2). */
        public static final TagKey<Item> ROCK_BAG_ITEMS = tag("rock_bag_items");
        /** What the durable rock bag carries: the rock bag's list plus the dirt, cobblestone, gravel, and sandstone kinds. */
        public static final TagKey<Item> DURABLE_ROCK_BAG_ITEMS = tag("durable_rock_bag_items");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name));
        }
    }
}
