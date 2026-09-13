package com.moostoet.pyrotech.library.fluid;

import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.items.ItemHandlerHelper;

/**
 * The 1.12 {@code InteractionExtinguishable} without its bounds: a held container that
 * can give a bucket of a dousing fluid gives it, and the caller puts its fire out. The
 * container empties on the server only; the client reports what the server will do.
 */
public final class Dousing {

    private Dousing() {
    }

    /** True when the held item can douse; on the server it has been drained by then. */
    public static boolean tryDouse(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            return false;
        }
        IFluidHandlerItem handler = FluidUtil.getFluidHandler(held.copyWithCount(1)).orElse(null);
        if (handler == null) {
            return false;
        }
        FluidStack drained = handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.SIMULATE);
        if (drained.getAmount() < FluidType.BUCKET_VOLUME || !drained.is(PyrotechTags.Fluids.DOUSING)) {
            return false;
        }
        if (player.level().isClientSide) {
            return true;
        }
        handler.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        ItemStack emptied = handler.getContainer();
        if (!player.isCreative()) {
            held.shrink(1);
            if (held.isEmpty()) {
                player.setItemInHand(hand, emptied);
            } else {
                ItemHandlerHelper.giveItemToPlayer(player, emptied);
            }
        }
        return true;
    }
}
