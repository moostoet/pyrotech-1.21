package com.moostoet.pyrotech.core.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.core.CoreBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Wet cob. It falls like sand when placed over air, but only then: 1.12 disabled the
 * neighbour-change fall check, so a wet cob whose support is dug out stays put until its
 * next random tick. It dries to cob over two random ticks, unless rain or more wet cob
 * sits on top.
 */
public final class WetCobBlock extends FallingBlock {

    public static final MapCodec<WetCobBlock> CODEC = simpleCodec(WetCobBlock::new);
    /** The 1.12 range was 0 to 3, though drying only ever reached 1. */
    public static final IntegerProperty DRY = IntegerProperty.create("dry", 0, 3);

    public WetCobBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DRY, 0));
    }

    @Override
    protected MapCodec<WetCobBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DRY);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        // FallingBlock schedules a fall check here; 1.12's cob did not.
        return state;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (isFree(level.getBlockState(pos.below())) && pos.getY() >= level.getMinBuildHeight()) {
            this.tick(state, level, pos, random);
            return;
        }
        BlockPos above = pos.above();
        if (level.isRainingAt(above) || level.getBlockState(above).is(this)) {
            return;
        }
        int dry = state.getValue(DRY);
        if (dry >= 1) {
            level.setBlockAndUpdate(pos, CoreBlocks.COB_DRY.get().defaultBlockState());
        } else {
            level.setBlockAndUpdate(pos, state.setValue(DRY, dry + 1));
        }
    }

    @Override
    public int getDustColor(BlockState state, BlockGetter level, BlockPos pos) {
        return 0xFF6B5A3E;
    }
}
