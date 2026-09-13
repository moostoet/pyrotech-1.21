package com.moostoet.pyrotech.storage.event;

import com.moostoet.pyrotech.storage.StorageConfig;
import com.moostoet.pyrotech.storage.item.BagItem;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-pickup: an open bag in a toggled location takes a picked-up item it can carry
 * before the inventory does, after the inventory's own stacks of it are topped up. The
 * event fires before vanilla's pickup delay check, so the delay is tested here.
 */
public final class BagPickupHandler {

    private static final int HOTBAR_SLOTS = 9;

    private BagPickupHandler() {
    }

    @SubscribeEvent
    public static void onPickup(ItemEntityPickupEvent.Pre event) {
        Player player = event.getPlayer();
        ItemEntity entity = event.getItemEntity();
        if (player.level().isClientSide || entity.hasPickUpDelay()) {
            return;
        }
        List<ItemStack> bags = locateBags(player);
        if (bags.isEmpty()) {
            return;
        }
        ItemStack stack = entity.getItem();
        boolean interested = false;
        for (ItemStack bag : bags) {
            if (BagItem.insert(bag, stack, true).getCount() != stack.getCount()) {
                interested = true;
                break;
            }
        }
        if (!interested) {
            return;
        }
        int original = stack.getCount();
        topUpInventory(player, stack);
        Level level = player.level();
        for (ItemStack bag : bags) {
            if (stack.isEmpty()) {
                break;
            }
            ItemStack remaining = BagItem.insert(bag, stack.copy(), false);
            if (remaining.getCount() != stack.getCount()) {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.4f,
                    ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1) * 2);
                stack.setCount(remaining.getCount());
            }
        }
        if (stack.isEmpty()) {
            event.setCanPickup(TriState.FALSE);
            player.take(entity, original);
            entity.discard();
        }
    }

    /** The inventory's existing stacks of the item fill up first, as 1.12 did. */
    private static void topUpInventory(Player player, ItemStack stack) {
        IItemHandler inventory = new PlayerMainInvWrapper(player.getInventory());
        for (int slot = 0; slot < inventory.getSlots() && !stack.isEmpty(); slot++) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                ItemStack remaining = inventory.insertItem(slot, stack.copy(), false);
                stack.setCount(remaining.getCount());
            }
        }
    }

    /** The open bags in the toggled locations: main hand, off hand, hotbar, and main inventory. */
    private static List<ItemStack> locateBags(Player player) {
        List<ItemStack> bags = new ArrayList<>();
        StorageConfig.Common config = StorageConfig.COMMON;
        Inventory inventory = player.getInventory();
        if (config.autoPickupMainHand.get()) {
            addBag(bags, player.getMainHandItem());
        }
        if (config.autoPickupOffHand.get()) {
            addBag(bags, player.getOffhandItem());
        }
        if (config.autoPickupHotbar.get()) {
            for (int slot = 0; slot < HOTBAR_SLOTS; slot++) {
                addBag(bags, inventory.items.get(slot));
            }
        }
        if (config.autoPickupInventory.get()) {
            for (int slot = HOTBAR_SLOTS; slot < inventory.items.size(); slot++) {
                addBag(bags, inventory.items.get(slot));
            }
        }
        return bags;
    }

    private static void addBag(List<ItemStack> bags, ItemStack stack) {
        if (stack.getItem() instanceof BagItem && BagItem.isOpen(stack) && !bags.contains(stack)) {
            bags.add(stack);
        }
    }
}
