package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The log pile: a pillar block with the 1.12 fire values. 1.12 also declared it wood for
 * leaf decay; the 1.21 equivalent is the {@code #minecraft:logs} tag, which would also make
 * it a log in every recipe, so it stays out and leaves do not count it as a trunk.
 */
public final class LogPileBlock extends RotatedPillarBlock {

    public LogPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return 5;
    }
}
