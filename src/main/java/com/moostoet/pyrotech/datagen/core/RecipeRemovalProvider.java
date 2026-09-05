package com.moostoet.pyrotech.datagen.core;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The removal list: the vanilla recipes Pyrotech drops so that its own route past each
 * gate is the only route. Each id becomes a {@code data/minecraft/recipe/<id>.json} stub
 * carrying only a {@code neoforge:false} condition, which NeoForge's recipe provider
 * cannot write. A user datapack can put any of them back. The list is the 76 translated
 * 1.12 ids plus the 45 additions settled in the progression skips and bloomery sign-offs.
 */
public final class RecipeRemovalProvider implements DataProvider {

    private static final List<String> REMOVED = List.of(
        // 1.12 lists, translated: wooden tools
        "wooden_axe", "wooden_hoe", "wooden_pickaxe", "wooden_shovel",
        // planks
        "oak_planks", "spruce_planks", "birch_planks", "jungle_planks", "acacia_planks", "dark_oak_planks",
        // wooden slabs
        "oak_slab", "spruce_slab", "birch_slab", "jungle_slab", "acacia_slab", "dark_oak_slab",
        // stone tools
        "stone_axe", "stone_pickaxe", "stone_hoe", "stone_shovel", "stone_sword",
        // bone meal
        "bone_meal", "bone_meal_from_bone_block",
        // stone variants
        "andesite", "granite", "diorite",
        // stone slabs
        "stone_slab", "sandstone_slab", "cobblestone_slab", "brick_slab", "stone_brick_slab",
        "nether_brick_slab", "quartz_slab", "red_sandstone_slab", "purpur_slab",
        // singles
        "stick", "clay", "snow_block", "bone_block", "paper", "torch", "coal_block", "coal", "chest",
        "furnace", "crafting_table", "redstone_block", "redstone", "lapis_block", "lapis_lazuli",
        "iron_nugget", "gold_nugget", "iron_ingot_from_iron_block", "gold_ingot_from_gold_block",
        "fire_charge", "leather", "item_frame", "book", "lead", "magma_cream", "arrow", "bread",
        "cookie", "cake", "shears",
        // boats
        "oak_boat", "spruce_boat", "birch_boat", "jungle_boat", "acacia_boat", "dark_oak_boat",
        // leather armor
        "leather_helmet", "leather_chestplate", "leather_leggings", "leather_boots",
        // furnace list
        "brick",
        // post-1.12 additions: planks
        "cherry_planks", "mangrove_planks", "bamboo_planks", "crimson_planks", "warped_planks",
        // wooden slabs
        "cherry_slab", "mangrove_slab", "bamboo_slab", "crimson_slab", "warped_slab",
        // sticks
        "stick_from_bamboo_item",
        // boats
        "cherry_boat", "mangrove_boat", "bamboo_raft",
        // cooking stations
        "campfire", "soul_campfire",
        // campfire cooking
        "baked_potato_from_campfire_cooking", "cooked_beef_from_campfire_cooking",
        "cooked_chicken_from_campfire_cooking", "cooked_cod_from_campfire_cooking",
        "cooked_mutton_from_campfire_cooking", "cooked_porkchop_from_campfire_cooking",
        "cooked_rabbit_from_campfire_cooking", "cooked_salmon_from_campfire_cooking",
        "dried_kelp_from_campfire_cooking",
        // storage and bone meal stations
        "barrel", "composter",
        // stonecutting slabs
        "stone_slab_from_stone_stonecutting", "stone_brick_slab_from_stone_stonecutting",
        "stone_brick_slab_from_stone_bricks_stonecutting", "cobblestone_slab_from_cobblestone_stonecutting",
        "sandstone_slab_from_sandstone_stonecutting", "red_sandstone_slab_from_red_sandstone_stonecutting",
        "brick_slab_from_bricks_stonecutting", "nether_brick_slab_from_nether_bricks_stonecutting",
        "quartz_slab_from_stonecutting", "purpur_slab_from_purpur_block_stonecutting",
        // smooth stone slab
        "smooth_stone_slab", "smooth_stone_slab_from_smooth_stone_stonecutting",
        // torches
        "soul_torch",
        // raw ore and copper decompressions (bloomery sign-off, item 7)
        "raw_iron", "raw_gold", "raw_copper", "copper_ingot", "copper_ingot_from_waxed_copper_block");

    private final PackOutput.PathProvider paths;

    public RecipeRemovalProvider(PackOutput output) {
        this.paths = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "neoforge:false");
        JsonArray conditions = new JsonArray();
        conditions.add(condition);
        JsonObject stub = new JsonObject();
        stub.add("neoforge:conditions", conditions);
        return CompletableFuture.allOf(REMOVED.stream()
            .map(id -> DataProvider.saveStable(cache, stub, this.paths.json(ResourceLocation.withDefaultNamespace(id))))
            .toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Pyrotech recipe removals";
    }
}
