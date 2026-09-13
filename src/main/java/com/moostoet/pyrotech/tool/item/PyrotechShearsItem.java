package com.moostoet.pyrotech.tool.item;

import com.moostoet.pyrotech.core.item.DurabilityTooltip;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Pyrotech shears: vanilla's with a 1.12 durability. Only the gold shears can go on an
 * enchanting table, at gold's enchantability; the rest are 0, as vanilla shears are.
 */
public class PyrotechShearsItem extends ShearsItem {

    private final int enchantmentValue;

    public PyrotechShearsItem(int enchantmentValue, Properties properties) {
        super(properties.component(DataComponents.TOOL, ShearsItem.createToolProperties()));
        this.enchantmentValue = enchantmentValue;
    }

    @Override
    public int getEnchantmentValue() {
        return this.enchantmentValue;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DurabilityTooltip.appendFull(stack, tooltip);
    }
}
