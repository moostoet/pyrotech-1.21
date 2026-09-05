package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The grass clump. In the dark under a light-blocking block it becomes a dirt clump; in
 * light it spreads grass to the dirt below and vanishes, or vanishes if the block below is
 * already grass. Bone meal does the same.
 */
public final class GrassRockBlock extends RockBlock implements BonemealableBlock {

    public GrassRockBlock(Properties properties) {
        super(false, properties);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        BlockPos above = pos.above();
        if (level.getMaxLocalRawBrightness(above) < 4 && level.getBlockState(above).getLightBlock(level, above) > 2) {
            level.setBlock(pos, CoreBlocks.ROCK_DIRT.get().defaultBlockState(), Block.UPDATE_ALL);
        } else if (level.getMaxLocalRawBrightness(pos) >= 9) {
            grow(level, pos);
        }
    }

    private static void grow(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState soil = level.getBlockState(below);
        if (soil.is(Blocks.DIRT)) {
            level.setBlock(below, Blocks.GRASS_BLOCK.defaultBlockState(), Block.UPDATE_ALL);
            level.removeBlock(pos, false);
        } else if (soil.is(Blocks.GRASS_BLOCK)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        grow(level, pos);
    }
}
