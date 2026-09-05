package com.moostoet.pyrotech.core.ignition;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The ignition hook (core sign-off, item 1). Igniter items, the powered igniter block, and
 * core's fire-adjacency listener ask it; refractory registers its pit burn into it. It
 * exists so that core and ignition never depend on refractory.
 */
public final class PyrotechIgnition {

    private static final List<BlockIgniter> IGNITERS = new CopyOnWriteArrayList<>();

    private PyrotechIgnition() {
    }

    public static void register(BlockIgniter igniter) {
        IGNITERS.add(igniter);
    }

    /** Asks each registered igniter in turn; returns true as soon as one starts a burn. */
    public static boolean tryIgnite(Level level, BlockPos pos) {
        for (BlockIgniter igniter : IGNITERS) {
            if (igniter.tryIgnite(level, pos)) {
                return true;
            }
        }
        return false;
    }
}
