package com.moostoet.pyrotech.tech.basic.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

/** The barrel's four item slots and its fluid. */
public record BarrelRecipeInput(List<ItemStack> items, FluidStack fluid) implements RecipeInput {

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < this.items.size() ? this.items.get(index) : ItemStack.EMPTY;
    }

    @Override
    public int size() {
        return this.items.size();
    }

    @Override
    public boolean isEmpty() {
        return this.fluid.isEmpty() || this.items.stream().allMatch(ItemStack::isEmpty);
    }
}
