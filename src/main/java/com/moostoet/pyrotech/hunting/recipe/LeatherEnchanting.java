package com.moostoet.pyrotech.hunting.recipe;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Set;

/**
 * What the durable upgrade and the fireproofing share: leather armour gets one level of an
 * enchantment and a dye, unless it already carries that enchantment or one it clashes with.
 */
final class LeatherEnchanting {

    private static final Set<net.minecraft.world.item.Item> LEATHER_ARMOR =
        Set.of(Items.LEATHER_HELMET, Items.LEATHER_CHESTPLATE, Items.LEATHER_LEGGINGS, Items.LEATHER_BOOTS);

    private LeatherEnchanting() {
    }

    static ItemStack findArmor(CraftingInput input) {
        for (ItemStack stack : input.items()) {
            if (LEATHER_ARMOR.contains(stack.getItem())) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    /** The enchanted, dyed copy, or nothing when the piece cannot take the enchantment. */
    static ItemStack enchant(ItemStack armor, ResourceKey<Enchantment> key, int color, HolderLookup.Provider registries) {
        if (armor.isEmpty() || !LEATHER_ARMOR.contains(armor.getItem())) {
            return ItemStack.EMPTY;
        }
        Holder<Enchantment> enchantment = registries.lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(key);
        ItemEnchantments present = EnchantmentHelper.getEnchantmentsForCrafting(armor);
        for (Holder<Enchantment> existing : present.keySet()) {
            if (existing.equals(enchantment) || !Enchantment.areCompatible(enchantment, existing)) {
                return ItemStack.EMPTY;
            }
        }
        ItemStack copy = armor.copyWithCount(1);
        EnchantmentHelper.updateEnchantments(copy, mutable -> mutable.upgrade(enchantment, 1));
        copy.set(DataComponents.DYED_COLOR, new DyedItemColor(color, true));
        return copy;
    }
}
