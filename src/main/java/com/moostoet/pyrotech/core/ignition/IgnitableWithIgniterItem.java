package com.moostoet.pyrotech.core.ignition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block a hand-held igniter can light. {@code facing} is the clicked face. */
public interface IgnitableWithIgniterItem {

    void igniteWithIgniterItem(Level level, BlockPos pos, BlockState state, Direction facing);
}
