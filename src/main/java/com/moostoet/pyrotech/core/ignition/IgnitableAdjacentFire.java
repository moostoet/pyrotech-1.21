package com.moostoet.pyrotech.core.ignition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** A block that fire next to it can light. Core's fire-adjacency listener calls it. */
public interface IgnitableAdjacentFire {

    /** {@code facing} points from this block toward the fire. */
    void igniteWithAdjacentFire(Level level, BlockPos pos, BlockState state, Direction facing);
}
