package com.moostoet.pyrotech.datagen;

import com.moostoet.pyrotech.datagen.bucket.BucketRecipeProvider;
import com.moostoet.pyrotech.datagen.core.CoreRecipeProvider;
import com.moostoet.pyrotech.datagen.hunting.HuntingRecipeProvider;
import com.moostoet.pyrotech.datagen.storage.StorageRecipeProvider;
import com.moostoet.pyrotech.datagen.tool.ToolRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * The one recipe provider the generator takes: vanilla names every {@code RecipeProvider}
 * "Recipes" and refuses a second. Each hoisting unit keeps its own recipe class, and this
 * one runs them in porting order.
 */
public final class PyrotechRecipeProvider extends RecipeProvider {

    private final List<RecipeUnit> units;

    public PyrotechRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
        this.units = List.of(new CoreRecipeProvider(output, registries), new ToolRecipeProvider(output, registries),
            new BucketRecipeProvider(output, registries), new StorageRecipeProvider(output, registries),
            new HuntingRecipeProvider(output, registries));
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        for (RecipeUnit unit : this.units) {
            unit.buildRecipes(output);
        }
    }
}
