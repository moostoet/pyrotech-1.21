package com.moostoet.pyrotech.storage.item;

import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.function.Predicate;

/**
 * A bag's inventory, athenaeum's {@code LargeDynamicItemLimitedStackHandler} with the bag
 * filter: no limit per slot, {@code capacity} items in total, one slot per kind of item
 * with the slots growing past the first ten as kinds are added, insertion into the first
 * slot that takes the item, and extraction from the last filled slot.
 */
public class BagStackHandler extends LargeStackHandler {

    public static final int INITIAL_SLOTS = 10;

    private final int capacity;
    private final Predicate<ItemStack> filter;

    public BagStackHandler(int capacity, Predicate<ItemStack> filter) {
        super(INITIAL_SLOTS, 1);
        this.capacity = capacity;
        this.filter = filter;
        this.setSize(INITIAL_SLOTS);
    }

    public int capacity() {
        return this.capacity;
    }

    public int remainingCapacity() {
        return this.capacity - this.totalCount();
    }

    @Override
    public void setSize(int size) {
        this.stacks = new GrowableList(size);
    }

    @Override
    public int getSlotLimit(int slot) {
        return Integer.MAX_VALUE;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return this.filter.test(stack);
    }

    /** Any slot means the bag: the item goes wherever it fits. */
    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return this.insert(stack, simulate);
    }

    public ItemStack insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty() || !this.filter.test(stack)) {
            return stack;
        }
        int room = this.remainingCapacity();
        if (room <= 0) {
            return stack;
        }
        int toInsert = Math.min(room, stack.getCount());
        ItemStack remaining = stack.copyWithCount(toInsert);
        for (int slot = 0; !remaining.isEmpty(); slot++) {
            if (slot >= this.getSlots()) {
                this.stacks.add(ItemStack.EMPTY);
            }
            remaining = super.insertItem(slot, remaining, simulate);
        }
        int left = stack.getCount() - toInsert;
        return left == 0 ? ItemStack.EMPTY : stack.copyWithCount(left);
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        for (int i = this.getSlots() - 1; i >= 0; i--) {
            if (!this.getStackInSlot(i).isEmpty()) {
                return super.extractItem(i, amount, simulate);
            }
        }
        return ItemStack.EMPTY;
    }

    private static final class GrowableList extends NonNullList<ItemStack> {

        GrowableList(int size) {
            super(new ArrayList<>(size), ItemStack.EMPTY);
            for (int i = 0; i < size; i++) {
                this.add(ItemStack.EMPTY);
            }
        }
    }
}
