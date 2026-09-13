package com.moostoet.pyrotech.storage.item;

import com.moostoet.pyrotech.storage.StorageComponents;
import com.moostoet.pyrotech.storage.block.TankBlock;
import com.moostoet.pyrotech.storage.block.entity.TankBlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.List;

/**
 * A tank in hand: it carries its fluid, joins the tanks it is placed against by the face
 * it was placed from, and hands back a copy drained by one bucket when crafted with.
 */
public final class TankBlockItem extends BlockItem {

    private final TankBlock tank;

    public TankBlockItem(TankBlock tank, Properties properties) {
        super(tank, properties);
        this.tank = tank;
    }

    /** The placed tank joins its neighbours once its fluid is in, which needs the clicked face. */
    @Override
    public InteractionResult place(BlockPlaceContext context) {
        InteractionResult result = super.place(context);
        if (result.consumesAction() && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof TankBlockEntity placed) {
            placed.updateConnectionsForPlacement(context.getClickedFace());
        }
        return result;
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        ItemStack remainder = stack.copyWithCount(1);
        IFluidHandlerItem handler = remainder.getCapability(Capabilities.FluidHandler.ITEM);
        if (handler != null) {
            handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        }
        return remainder;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        SimpleFluidContent content = stack.get(StorageComponents.TANK_FLUID);
        if (content == null || content.isEmpty()) {
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.fluid.capacity", this.tank.capacity()));
        } else {
            FluidStack fluid = content.copy();
            tooltip.add(Component.translatable("gui.pyrotech.tooltip.fluid", fluid.getHoverName(), fluid.getAmount(), this.tank.capacity()));
        }
        boolean hot = this.tank.holdsHotFluids();
        tooltip.add(Component.translatable("gui.pyrotech.tooltip.hot.fluids." + hot).withStyle(hot ? ChatFormatting.GREEN : ChatFormatting.RED));
        tooltip.add(Component.translatable("gui.pyrotech.tooltip.contents.retain.true").withStyle(ChatFormatting.GREEN));
    }
}
