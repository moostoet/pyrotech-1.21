package com.moostoet.pyrotech.core.item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The crafting table template's item. A recipe that lists it gives it back whole, which
 * is what the 1.12 {@code CraftingTableRecipe} did for the crafting table, so that recipe
 * is plain JSON here.
 */
public final class CraftingTableTemplateItem extends BlockItem {

    public CraftingTableTemplateItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return stack.copyWithCount(1);
    }
}
