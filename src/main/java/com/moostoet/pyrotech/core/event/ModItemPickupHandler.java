package com.moostoet.pyrotech.core.event;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.CoreTriggers;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Fires the root advancement's trigger when a player picks up any Pyrotech item. */
public final class ModItemPickupHandler {

    private ModItemPickupHandler() {
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Post event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack stack = event.getOriginalStack();
        if (!stack.isEmpty() && BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace().equals(Pyrotech.MOD_ID)) {
            CoreTriggers.PICKUP_MOD_ITEM.get().trigger(player);
        }
    }
}
