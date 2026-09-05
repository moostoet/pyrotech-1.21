package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** Cob. Rain on top, or wet cob on top, turns it back into wet cob on a random tick. */
public final class DryCobBlock extends Block {

    public DryCobBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.isRainingAt(above) || level.getBlockState(above).is(CoreBlocks.COB_WET.get())) {
            level.setBlockAndUpdate(pos, CoreBlocks.COB_WET.get().defaultBlockState());
        }
    }
}
