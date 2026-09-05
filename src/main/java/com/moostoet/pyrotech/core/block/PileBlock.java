package com.moostoet.pyrotech.core.block;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The 1.12 {@code BlockPileBase}: an eight-level heap that a player takes apart one level
 * at a time. Each harvested level spawns one drop on top of the pile; the last level
 * removes the block. Breaking without the right tool lowers the pile and drops nothing.
 * The {@code level} property counts down from a full block: 0 is full, 7 is the last
 * eighth, as the migrated blockstates expect.
 */
public abstract class PileBlock extends Block {

    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, 7);
    public static final int MAX_LEVEL = 8;

    private static final VoxelShape[] SHAPES = new VoxelShape[MAX_LEVEL];

    static {
        for (int i = 0; i < MAX_LEVEL; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, (i + 1) * 2, 16);
        }
    }

    protected PileBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(LEVEL, 0));
    }

    /** The pile's height in eighths, 1 to 8, or 0 when the state is not a pile. */
    public static int getLevel(BlockState state) {
        return state.getBlock() instanceof PileBlock ? MAX_LEVEL - state.getValue(LEVEL) : 0;
    }

    /** The same pile at a height of {@code level} eighths, clamped to 1 to 8. */
    public static BlockState withLevel(BlockState state, int level) {
        return state.getBlock() instanceof PileBlock ? state.setValue(LEVEL, MAX_LEVEL - Mth.clamp(level, 1, MAX_LEVEL)) : state;
    }

    /** The item one harvested level gives. */
    protected abstract ItemStack getDrop(Level level, BlockPos pos, BlockState state);

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[getLevel(state) - 1];
    }

    @Override
    public boolean onDestroyedByPlayer(BlockState state, Level level, BlockPos pos, Player player, boolean willHarvest, FluidState fluid) {
        if (!willHarvest) {
            if (!level.isClientSide) {
                lower(level, pos, state);
            }
            return false;
        }
        return true;
    }

    @Override
    public void playerDestroy(Level level, Player player, BlockPos pos, BlockState state, @Nullable BlockEntity blockEntity, ItemStack tool) {
        super.playerDestroy(level, player, pos, state, blockEntity, tool);
        if (!level.isClientSide) {
            ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5 + getLevel(state) * 2 / 16.0, pos.getZ() + 0.5,
                this.getDrop(level, pos, state));
            drop.setDeltaMovement(0, 0.1, 0);
            level.addFreshEntity(drop);
            lower(level, pos, state);
        }
    }

    private static void lower(Level level, BlockPos pos, BlockState state) {
        int height = getLevel(state);
        if (height <= 1) {
            level.removeBlock(pos, false);
        } else {
            level.setBlock(pos, withLevel(state, height - 1), Block.UPDATE_ALL);
        }
    }
}
