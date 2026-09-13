package com.moostoet.pyrotech.storage.item;

import com.mojang.serialization.Codec;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * What a bag item carries: its non-empty stacks, in slot order, with their large counts.
 * Vanilla's container component cannot hold them, since it caps every slot at 99
 * (storage sign-off, item 1).
 */
public record BagContents(List<ItemStack> stacks) {

    public static final BagContents EMPTY = new BagContents(List.of());
    public static final Codec<BagContents> CODEC = LargeStackHandler.STACK_CODEC.listOf().xmap(BagContents::new, BagContents::stacks);

    public static BagContents of(BagStackHandler handler) {
        List<ItemStack> stacks = new ArrayList<>();
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                stacks.add(stack.copy());
            }
        }
        return stacks.isEmpty() ? EMPTY : new BagContents(List.copyOf(stacks));
    }

    /** Replaces the handler's slots with these stacks. */
    public void copyInto(BagStackHandler handler) {
        handler.setSize(Math.max(BagStackHandler.INITIAL_SLOTS, this.stacks.size()));
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            handler.setStackInSlot(slot, this.stacks.get(slot).copy());
        }
    }

    public int totalCount() {
        int total = 0;
        for (ItemStack stack : this.stacks) {
            total += stack.getCount();
        }
        return total;
    }

    public boolean isEmpty() {
        return this.stacks.isEmpty();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof BagContents contents && ItemStack.listMatches(this.stacks, contents.stacks);
    }

    @Override
    public int hashCode() {
        return ItemStack.hashStackList(this.stacks);
    }
}
