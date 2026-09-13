package com.moostoet.pyrotech.library.interaction;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * What a click and a scroll on a slot move, as athenaeum's {@code InteractionItemStack}
 * had it: a click puts the whole held stack in or takes one full stack out, a scroll moves
 * one item. Insertion on the client is a probe, so both sides agree on the result and the
 * server alone changes anything.
 */
public final class SlotInteraction {

    private SlotInteraction() {
    }

    /** Moves up to {@code count} of the held stack into the slot. True when any moved. */
    public static boolean insert(IItemHandler handler, int slot, Player player, ItemStack held, int count) {
        if (slot < 0 || held.isEmpty()) {
            return false;
        }
        boolean simulate = player.level().isClientSide;
        ItemStack offered = held.copyWithCount(Math.min(count, held.getCount()));
        ItemStack remainder = handler.insertItem(slot, offered, simulate);
        int inserted = offered.getCount() - remainder.getCount();
        if (inserted <= 0) {
            return false;
        }
        if (!simulate) {
            held.shrink(inserted);
        }
        return true;
    }

    /** The whole held stack into the slot. */
    public static boolean insert(IItemHandler handler, int slot, Player player, ItemStack held) {
        return insert(handler, slot, player, held, Integer.MAX_VALUE);
    }

    /**
     * One of the held item into the slot, or, when the slot already holds something the
     * held item cannot join, one of that item from anywhere in the player's inventory.
     */
    public static boolean scrollInsert(IItemHandler handler, int slot, Player player) {
        ItemStack held = player.getMainHandItem();
        if (!handler.getStackInSlot(slot).isEmpty() && handler.insertItem(slot, held, true).getCount() == held.getCount()) {
            for (ItemStack stack : player.getInventory().items) {
                if (!stack.isEmpty() && handler.insertItem(slot, stack, true).getCount() != stack.getCount()) {
                    return insert(handler, slot, player, stack, 1);
                }
            }
        }
        return insert(handler, slot, player, held, 1);
    }

    /** A click with an empty hand: one full stack out of the slot, with the pickup sounds. */
    public static boolean extract(IItemHandler handler, int slot, Player player, BlockPos pos) {
        return extract(handler, slot, player, pos, Integer.MAX_VALUE, true);
    }

    /** A scroll down: one item out of the slot, quietly. */
    public static boolean extractOne(IItemHandler handler, int slot, Player player, BlockPos pos) {
        return extract(handler, slot, player, pos, 1, false);
    }

    /**
     * Takes up to {@code count} out of the slot into the player's inventory, or onto the
     * block when the inventory is full. True when any moved.
     */
    private static boolean extract(IItemHandler handler, int slot, Player player, BlockPos pos, int count, boolean sound) {
        if (slot < 0) {
            return false;
        }
        Level level = player.level();
        ItemStack taken = handler.extractItem(slot, count, level.isClientSide);
        if (taken.isEmpty()) {
            return false;
        }
        if (!level.isClientSide) {
            giveOrDrop(player, taken, pos, sound);
            if (sound) {
                level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.25f,
                    (float) (1 + level.random.nextGaussian() * 0.4));
            }
        }
        return true;
    }

    /** The 1.12 {@code addToInventoryOrSpawn}: into the inventory, with the pickup sound, the rest dropped on the block. */
    public static void giveOrDrop(Player player, ItemStack stack, BlockPos pos, boolean sound) {
        Level level = player.level();
        if (player.getInventory().add(stack) && sound) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2f,
                ((level.random.nextFloat() - level.random.nextFloat()) * 0.7f + 1) * 2);
        }
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, stack);
        }
    }
}
