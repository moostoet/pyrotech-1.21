package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Living tar: fire on its top face burns forever, and the block itself never catches. */
public final class LivingTarBlock extends Block {

    public LivingTarBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFireSource(BlockState state, LevelReader level, BlockPos pos, Direction direction) {
        return direction == Direction.UP;
    }
}
