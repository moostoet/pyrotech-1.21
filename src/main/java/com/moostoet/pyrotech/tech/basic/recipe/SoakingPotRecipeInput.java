package com.moostoet.pyrotech.tech.basic.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

/** The item in the soaking pot and the fluid it sits in. */
public record SoakingPotRecipeInput(ItemStack item, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index == 0 ? this.item : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return 1;
    }

    @Override
    public boolean isEmpty() {
        return this.item.isEmpty() || this.fluid.isEmpty();
    }
}
