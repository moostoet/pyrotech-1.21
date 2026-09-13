package com.moostoet.pyrotech.core.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * A crafting-grid hammer. It is not a mining tool: a recipe that lists it damages it by
 * one instead of consuming it, through the stack-sensitive crafting remainder, so the
 * hammer recipes stay plain JSON. Which items count as hammers is the
 * {@code #pyrotech:hammers} tag; their level is the {@code pyrotech:tool_levels} data map.
 */
public class HammerItem extends Item {

    public HammerItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return CraftingRemainders.damaged(stack, 1);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        DurabilityTooltip.appendFull(stack, tooltip);
    }
}
