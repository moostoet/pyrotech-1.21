package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import java.util.List;

/** The barrel's item: a sealed one lists its fluid and items behind shift, and every one says it breaks with hot fluids. */
public final class BarrelItem extends BlockItem {

    public BarrelItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        SimpleFluidContent fluid = stack.get(TechBasicComponents.BARREL_FLUID.get());
        ItemContainerContents contents = stack.get(DataComponents.CONTAINER);
        if (fluid != null || contents != null) {
            if (Screen.hasShiftDown()) {
                FluidStack held = fluid == null ? FluidStack.EMPTY : fluid.copy();
                if (!held.isEmpty()) {
                    tooltip.add(Component.translatable("gui.pyrotech.tooltip.fluid", held.getHoverName(), held.getAmount(), FluidType.BUCKET_VOLUME)
                        .withStyle(ChatFormatting.GRAY));
                }
                if (contents != null) {
                    for (ItemStack item : contents.nonEmptyItems()) {
                        tooltip.add(item.getHoverName().copy().withStyle(ChatFormatting.GRAY));
                    }
                }
            } else {
                tooltip.add(Component.translatable("gui.pyrotech.tooltip.extended.shift", ChatFormatting.GOLD, ChatFormatting.GRAY)
                    .withStyle(ChatFormatting.GRAY));
            }
        }
        tooltip.add(Component.translatable("gui.pyrotech.tooltip.hot.fluids.false").withStyle(ChatFormatting.RED));
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
