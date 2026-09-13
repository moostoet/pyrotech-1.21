package com.moostoet.pyrotech.tool.item;

import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/** A tool that may carry a redstone or quartz behaviour; the behaviour calls back when the state flips. */
public interface ActiveTool {

    @Nullable
    ActiveBehaviour activeBehaviour();

    default void onActiveChanged(ItemStack stack) {
    }
}
