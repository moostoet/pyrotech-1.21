package com.moostoet.pyrotech.tech.basic.recipe;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

/**
 * One ingredient in, one stack out: the shape of the kiln, drying, chopping block,
 * compacting bin, tanning rack, campfire, and compost bin recipes. The subclasses add
 * their numbers.
 */
public abstract class SingleIngredientRecipe implements Recipe<SingleRecipeInput> {

    protected final Ingredient ingredient;
    protected final ItemStack result;

    protected SingleIngredientRecipe(Ingredient ingredient, ItemStack result) {
        this.ingredient = ingredient;
        this.result = result;
    }

    public Ingredient ingredient() {
        return this.ingredient;
    }

    public ItemStack result() {
        return this.result;
    }

    @Override
    public boolean matches(SingleRecipeInput input, Level level) {
        return this.ingredient.test(input.item());
    }

    @Override
    public ItemStack assemble(SingleRecipeInput input, HolderLookup.Provider registries) {
        return this.result.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return this.result;
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return NonNullList.of(Ingredient.EMPTY, this.ingredient);
    }
}
