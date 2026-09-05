package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * A rock on the ground: the 1.12 {@code BlockRock} variants, {@code BlockRockGrass}, and
 * {@code BlockRockNetherrack}, one block each. A sixteenth high, no collision, replaceable,
 * and it needs a solid face below or it pops off. The rock of wood chips needs a shovel to
 * be picked up while the tweak is on.
 */
public class RockBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    private final boolean woodChips;

    public RockBlock(boolean woodChips, Properties properties) {
        super(properties);
        this.woodChips = woodChips;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (!level.isClientSide && !state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        if (this.woodChips && CoreConfig.COMMON.requireShovelToPickupWoodChips.get()) {
            return player.getMainHandItem().canPerformAction(ItemAbilities.SHOVEL_DIG);
        }
        return super.canHarvestBlock(state, level, pos, player);
    }
}
