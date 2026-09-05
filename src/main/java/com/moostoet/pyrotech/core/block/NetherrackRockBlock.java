package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.PyrotechTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * The netherrack gib. On a random tick it turns the full block below into netherrack when
 * that block is in {@code #pyrotech:netherrack_spreads_to}, then converts one more such
 * block within the spread radius that already touches netherrack. The radius is the 1.12
 * config default, now a constant.
 */
public final class NetherrackRockBlock extends RockBlock {

    private static final int SPREAD_RADIUS = 3;

    public NetherrackRockBlock(Properties properties) {
        super(false, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos below = pos.below();
        if (spreadsTo(level, below)) {
            level.setBlock(below, Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL);
        }

        List<BlockPos> candidates = new ArrayList<>();
        for (BlockPos candidate : BlockPos.betweenClosed(pos.offset(-SPREAD_RADIUS, -SPREAD_RADIUS, -SPREAD_RADIUS),
            pos.offset(SPREAD_RADIUS, SPREAD_RADIUS, SPREAD_RADIUS))) {
            if (candidate.distSqr(pos) <= SPREAD_RADIUS * SPREAD_RADIUS) {
                candidates.add(candidate.immutable());
            }
        }
        Collections.shuffle(candidates, new Random(random.nextLong()));
        for (BlockPos candidate : candidates) {
            if (spreadsTo(level, candidate) && touchesNetherrack(level, candidate)) {
                level.setBlock(candidate, Blocks.NETHERRACK.defaultBlockState(), Block.UPDATE_ALL);
                return;
            }
        }
    }

    private static boolean spreadsTo(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.is(PyrotechTags.Blocks.NETHERRACK_SPREADS_TO) && state.isCollisionShapeFullBlock(level, pos);
    }

    private static boolean touchesNetherrack(ServerLevel level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getBlockState(pos.relative(direction)).is(Blocks.NETHERRACK)) {
                return true;
            }
        }
        return false;
    }
}
