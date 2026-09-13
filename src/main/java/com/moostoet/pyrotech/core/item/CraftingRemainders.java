package com.moostoet.pyrotech.core.item;

import net.minecraft.world.item.ItemStack;

/** The crafting remainders of tools that a recipe wears instead of consuming. */
public final class CraftingRemainders {

    private CraftingRemainders() {
    }

    /**
     * One of the stack with {@code amount} more damage, or nothing once that breaks it. An
     * item without durability comes back untouched.
     */
    public static ItemStack damaged(ItemStack stack, int amount) {
        ItemStack remainder = stack.copyWithCount(1);
        if (!remainder.isDamageableItem()) {
            return remainder;
        }
        remainder.setDamageValue(remainder.getDamageValue() + amount);
        return remainder.getDamageValue() >= remainder.getMaxDamage() ? ItemStack.EMPTY : remainder;
    }
}
