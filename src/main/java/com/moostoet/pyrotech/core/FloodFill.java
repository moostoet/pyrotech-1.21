package com.moostoet.pyrotech.core;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelAccessor;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

/**
 * The 1.12 breadth-first flood fill. From a start position it visits every block the
 * candidate test accepts, connected through the six faces, and runs the action on each
 * until the limit is spent or the action asks to stop. Worldgen's dense coal is the
 * first caller; the pit burn is the next.
 */
public final class FloodFill {

    public interface Candidate {

        boolean test(LevelAccessor level, BlockPos pos);
    }

    public interface Action {

        /** @return false to stop the fill */
        boolean apply(LevelAccessor level, BlockPos pos);
    }

    private FloodFill() {
    }

    /** @return whether any block passed the candidate test */
    public static boolean apply(LevelAccessor level, BlockPos start, Candidate candidate, Action action, int limit) {
        Deque<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        boolean found = false;
        queue.offer(start.immutable());
        BlockPos pos;
        while (limit > 0 && (pos = queue.poll()) != null) {
            if (!visited.add(pos) || !candidate.test(level, pos)) {
                continue;
            }
            found = true;
            if (!action.apply(level, pos)) {
                break;
            }
            limit--;
            for (Direction direction : Direction.values()) {
                BlockPos neighbour = pos.relative(direction);
                if (!visited.contains(neighbour)) {
                    queue.offer(neighbour);
                }
            }
        }
        return found;
    }
}
