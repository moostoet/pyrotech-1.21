package com.moostoet.pyrotech.datagen;

import net.minecraft.data.recipes.RecipeOutput;

/** A hoisting unit's recipe class, run by {@link PyrotechRecipeProvider}. */
public interface RecipeUnit {

    void buildRecipes(RecipeOutput output);
}
