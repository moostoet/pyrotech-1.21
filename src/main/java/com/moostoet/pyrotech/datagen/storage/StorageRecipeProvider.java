package com.moostoet.pyrotech.datagen.storage;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import com.moostoet.pyrotech.storage.StorageBlocks;
import com.moostoet.pyrotech.storage.recipe.TankFlushRecipe;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.common.Tags;

import java.util.concurrent.CompletableFuture;

/**
 * Storage's recipes: the thirteen 1.12 shaped JSONs and the two tank flush recipes, with
 * no condition (recipe sign-off). Ids are the 1.12 file names; the ore dictionary names are
 * the vanilla tags, the materials core's items.
 */
public final class StorageRecipeProvider extends RecipeProvider implements RecipeUnit {

    public StorageRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        Item stoneBrick = item(Material.BRICK_STONE);
        Item tarredBoard = item(Material.BOARD_TARRED);
        Item refractoryBrick = item(Material.REFRACTORY_BRICK);

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.STASH.get()).pattern("S S").pattern("SSS")
            .define('S', ItemTags.WOODEN_SLABS)
            .unlockedBy("has_wooden_slab", has(ItemTags.WOODEN_SLABS)).save(output, id("stash"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.SHELF.get()).pattern("P-P").pattern("P-P").pattern("P-P")
            .define('P', ItemTags.PLANKS).define('-', ItemTags.WOODEN_SLABS)
            .unlockedBy("has_wooden_slab", has(ItemTags.WOODEN_SLABS)).save(output, id("shelf"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.CRATE.get()).pattern("PPP").pattern("PSP").pattern("PPP")
            .define('P', ItemTags.PLANKS).define('S', ItemTags.WOODEN_SLABS)
            .unlockedBy("has_wooden_slab", has(ItemTags.WOODEN_SLABS)).save(output, id("crate"));
        durable(output, StorageBlocks.STASH.get(), StorageBlocks.STASH_STONE.get(), stoneBrick, tarredBoard, "stash_durable");
        durable(output, StorageBlocks.SHELF.get(), StorageBlocks.SHELF_STONE.get(), stoneBrick, tarredBoard, "shelf_durable");
        durable(output, StorageBlocks.CRATE.get(), StorageBlocks.CRATE_STONE.get(), stoneBrick, tarredBoard, "crate_durable");
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.WOOD_RACK.get()).pattern("OSO").pattern("LSL").pattern("OSO")
            .define('O', ItemTags.LOGS).define('S', ItemTags.WOODEN_SLABS).define('L', Items.LADDER)
            .unlockedBy(getHasName(Items.LADDER), has(Items.LADDER)).save(output, id("wood_rack"));

        Item twine = item(Material.TWINE);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.BAG_SIMPLE.get()).pattern("sWs").pattern("WSW").pattern("sWs")
            .define('s', twine).define('W', ItemTags.WOOL).define('S', StorageBlocks.STASH.get())
            .unlockedBy(getHasName(StorageBlocks.STASH.get()), has(StorageBlocks.STASH.get())).save(output, id("bag_simple"));
        Item cord = item(Material.LEATHER_DURABLE_CORD);
        Item sheet = item(Material.LEATHER_DURABLE_SHEET);
        Item strap = item(Material.LEATHER_DURABLE_STRAP);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.BAG_DURABLE.get()).pattern("sCs").pattern("LSL").pattern("sLs")
            .define('s', strap).define('C', cord).define('L', sheet).define('S', StorageBlocks.STASH_STONE.get())
            .unlockedBy(getHasName(StorageBlocks.STASH_STONE.get()), has(StorageBlocks.STASH_STONE.get())).save(output, id("bag_durable"));

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.STONE_TANK.get()).pattern("BGB").pattern("G G").pattern("BGB")
            .define('B', stoneBrick).define('G', Tags.Items.GLASS_BLOCKS)
            .unlockedBy(getHasName(stoneBrick), has(stoneBrick)).save(output, id("stone_tank"));
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.BRICK_TANK.get()).pattern("BGB").pattern("G G").pattern("BGB")
            .define('B', refractoryBrick).define('G', CoreBlocks.REFRACTORY_GLASS.get())
            .unlockedBy(getHasName(refractoryBrick), has(refractoryBrick)).save(output, id("brick_tank"));
        Item clayLump = item(Material.CLAY_LUMP);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.FAUCET_STONE.get()).pattern("S S").pattern("CSC")
            .define('S', stoneBrick).define('C', clayLump)
            .unlockedBy(getHasName(stoneBrick), has(stoneBrick)).save(output, id("stone_faucet"));
        Item refractoryClayLump = item(Material.REFRACTORY_CLAY_LUMP);
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, StorageBlocks.FAUCET_BRICK.get()).pattern("S S").pattern("CSC")
            .define('S', refractoryBrick).define('C', refractoryClayLump)
            .unlockedBy(getHasName(refractoryBrick), has(refractoryBrick)).save(output, id("brick_faucet"));

        output.accept(id("stone_tank_empty"), new TankFlushRecipe(Ingredient.of(StorageBlocks.STONE_TANK.get())), null);
        output.accept(id("brick_tank_empty"), new TankFlushRecipe(Ingredient.of(StorageBlocks.BRICK_TANK.get())), null);
    }

    /** The 1.12 durable upgrade: the wood block ringed by stone bricks and tarred boards. */
    private static void durable(RecipeOutput output, ItemLike wood, ItemLike durable, Item stoneBrick, Item tarredBoard, String name) {
        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, durable).pattern("SBS").pattern("BXB").pattern("SBS")
            .define('S', stoneBrick).define('B', tarredBoard).define('X', wood)
            .unlockedBy(getHasName(wood), has(wood)).save(output, id(name));
    }

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
