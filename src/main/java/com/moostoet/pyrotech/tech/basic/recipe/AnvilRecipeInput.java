package com.moostoet.pyrotech.tech.basic.recipe;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;
import org.jetbrains.annotations.Nullable;

/** The item on the anvil, the anvil's tier, and the tool in hand, or no tool when only the item is being checked. */
public record AnvilRecipeInput(ItemStack item, AnvilTier tier, @Nullable AnvilToolType tool) implements RecipeInput {

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
        return this.item.isEmpty();
    }
}
