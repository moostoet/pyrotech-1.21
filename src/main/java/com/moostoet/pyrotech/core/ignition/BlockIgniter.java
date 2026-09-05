package com.moostoet.pyrotech.core.ignition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * One answer to the ignition hook: "can this position start a burn, and if so, start it".
 * Refractory registers the pit burn; nothing in core answers.
 */
@FunctionalInterface
public interface BlockIgniter {

    /** Returns true when a burn started at {@code pos}. */
    boolean tryIgnite(Level level, BlockPos pos);
}
