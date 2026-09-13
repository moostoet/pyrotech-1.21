package com.moostoet.pyrotech.tech.basic.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;

/** Athenaeum's {@code StackHelper.spawnStackOnTop}: an item entity centred on the block, one block up, plus an offset. */
public final class Spill {

    private Spill() {
    }

    public static void onTop(Level level, BlockPos pos, ItemStack stack, double offsetY) {
        if (!stack.isEmpty()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1 + offsetY, pos.getZ() + 0.5, stack);
        }
    }

    public static void onTop(Level level, BlockPos pos, ItemStack stack) {
        onTop(level, pos, stack, 0);
    }

    /** Every slot of a handler onto the block, emptying it. */
    public static void handler(Level level, BlockPos pos, IItemHandler handler, double offsetY) {
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            onTop(level, pos, handler.extractItem(slot, Integer.MAX_VALUE, false), offsetY);
        }
    }
}
