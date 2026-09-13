package com.moostoet.pyrotech.datagen.bucket;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.bucket.BucketItems;
import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import com.moostoet.pyrotech.datagen.RecipeUnit;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.concurrent.CompletableFuture;

/**
 * Bucket's recipes: the four 1.12 crafting JSONs and the two furnace recipes
 * {@code VanillaFurnaceRecipesAdd} registered in code, with no condition (bucket sign-off,
 * item 5). Ids are the 1.12 file names; {@code plankWood} is the vanilla planks tag and
 * the materials are core's items. The pit kiln recipes for the fired buckets are tech/basic's.
 */
public final class BucketRecipeProvider extends RecipeProvider implements RecipeUnit {

    private static final int SMELT_TICKS = 200;

    public BucketRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public void buildRecipes(RecipeOutput output) {
        Item fibers = item(Material.PLANT_FIBERS);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BucketItems.BUCKET_WOOD.get()).pattern("MFM").pattern(" M ")
            .define('M', ItemTags.PLANKS).define('F', fibers)
            .unlockedBy(getHasName(fibers), has(fibers)).save(output, id("bucket_wood"));
        Item unfiredBrick = item(Material.UNFIRED_BRICK);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BucketItems.BUCKET_CLAY_UNFIRED.get()).pattern("M M").pattern(" M ")
            .define('M', unfiredBrick)
            .unlockedBy(getHasName(unfiredBrick), has(unfiredBrick)).save(output, id("bucket_clay_unfired"));
        Item stoneBrick = item(Material.BRICK_STONE);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BucketItems.BUCKET_STONE.get()).pattern("MFM").pattern("FMF")
            .define('M', stoneBrick).define('F', Items.CLAY_BALL)
            .unlockedBy(getHasName(stoneBrick), has(stoneBrick)).save(output, id("bucket_stone"));
        Item unfiredRefractoryBrick = item(Material.UNFIRED_REFRACTORY_BRICK);
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, BucketItems.BUCKET_REFRACTORY_UNFIRED.get()).pattern("M M").pattern(" M ")
            .define('M', unfiredRefractoryBrick)
            .unlockedBy(getHasName(unfiredRefractoryBrick), has(unfiredRefractoryBrick)).save(output, id("bucket_refractory_unfired"));
        smelt(output, BucketItems.BUCKET_CLAY_UNFIRED.get(), BucketItems.BUCKET_CLAY.get());
        smelt(output, BucketItems.BUCKET_REFRACTORY_UNFIRED.get(), BucketItems.BUCKET_REFRACTORY.get());
    }

    private static void smelt(RecipeOutput output, Item unfired, Item fired) {
        SimpleCookingRecipeBuilder.smelting(Ingredient.of(unfired), RecipeCategory.TOOLS, fired, 0.1f, SMELT_TICKS)
            .unlockedBy(getHasName(unfired), has(unfired)).save(output, id(getItemName(fired) + "_from_smelting"));
    }

    private static Item item(Material material) {
        return CoreItems.material(material).get();
    }

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(Pyrotech.MOD_ID, name);
    }
}
