package com.moostoet.pyrotech.tech.basic;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/** The tags tech/basic owns; tech/basic's datagen writes them. */
public final class TechBasicTags {

    private TechBasicTags() {
    }

    public static final class Items {

        /** What the campfire and the pit kiln burn: {@code #minecraft:logs_that_burn} (tech/basic sign-off, item 5). */
        public static final TagKey<Item> CAMPFIRE_FUELS = tag("campfire_fuels");
        /** The smelting foods the campfire refuses to cook: bread and cookie. */
        public static final TagKey<Item> CAMPFIRE_BLACKLIST = tag("campfire_blacklist");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name));
        }
    }
}
