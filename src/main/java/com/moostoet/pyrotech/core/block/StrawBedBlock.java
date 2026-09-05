package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.event.StrawBedHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BedPart;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * The straw bed: two quarter-high blocks, foot and head, that a player can sleep in
 * without setting a spawn point, and that is destroyed once slept in (see
 * {@link StrawBedHandler}). It is a bed to the sleep code through NeoForge's block hooks,
 * not a {@link BedBlock}, so it keeps its own model and stays out of the village bed tag.
 */
public final class StrawBedBlock extends Block {

    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final EnumProperty<BedPart> PART = BedBlock.PART;
    public static final BooleanProperty OCCUPIED = BedBlock.OCCUPIED;

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 4, 16);
    private static final float EXPLOSION_RADIUS = 5.0F;

    public StrawBedBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
            .setValue(FACING, Direction.NORTH)
            .setValue(PART, BedPart.FOOT)
            .setValue(OCCUPIED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART, OCCUPIED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    // -- Bed hooks -----------------------------------------------------------

    @Override
    public boolean isBed(BlockState state, BlockGetter level, BlockPos pos, @Nullable LivingEntity sleeper) {
        return true;
    }

    @Override
    public void setBedOccupied(BlockState state, Level level, BlockPos pos, LivingEntity sleeper, boolean occupied) {
        level.setBlock(pos, state.setValue(OCCUPIED, occupied), Block.UPDATE_ALL);
    }

    @Override
    public Direction getBedDirection(BlockState state, LevelReader level, BlockPos pos) {
        return state.getValue(FACING);
    }

    /** The direction from this part to the other one. */
    private static Direction toOtherPart(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == BedPart.FOOT ? facing : facing.getOpposite();
    }

    // -- Sleeping ------------------------------------------------------------

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.CONSUME;
        }
        if (state.getValue(PART) == BedPart.FOOT) {
            pos = pos.relative(state.getValue(FACING));
            state = level.getBlockState(pos);
            if (!state.is(this)) {
                return InteractionResult.CONSUME;
            }
        }
        if (!BedBlock.canSetSpawn(level)) {
            this.explode(level, pos, state);
            return InteractionResult.SUCCESS;
        }
        if (state.getValue(OCCUPIED)) {
            if (isAnyoneSleepingAt(level, pos)) {
                player.displayClientMessage(Component.translatable("block.minecraft.bed.occupied"), true);
                return InteractionResult.SUCCESS;
            }
            state = state.setValue(OCCUPIED, false);
            level.setBlock(pos, state, Block.UPDATE_INVISIBLE);
        }
        BlockPos bedPos = pos;
        player.startSleepInBed(bedPos).ifLeft(problem -> {
            if (problem.getMessage() != null) {
                player.displayClientMessage(problem.getMessage(), true);
            }
        }).ifRight(unit -> StrawBedHandler.markUsed(level, bedPos));
        return InteractionResult.SUCCESS;
    }

    private static boolean isAnyoneSleepingAt(Level level, BlockPos pos) {
        for (Player player : level.players()) {
            if (player.isSleeping() && player.getSleepingPos().filter(pos::equals).isPresent()) {
                return true;
            }
        }
        return false;
    }

    private void explode(Level level, BlockPos pos, BlockState state) {
        level.removeBlock(pos, false);
        BlockPos other = pos.relative(toOtherPart(state));
        if (level.getBlockState(other).is(this)) {
            level.removeBlock(other, false);
        }
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_RADIUS, true, Level.ExplosionInteraction.BLOCK);
    }

    // -- The two halves ------------------------------------------------------

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (direction == toOtherPart(state)) {
            return neighborState.is(this) && neighborState.getValue(PART) != state.getValue(PART)
                ? state.setValue(OCCUPIED, neighborState.getValue(OCCUPIED))
                : Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide && player.isCreative() && state.getValue(PART) == BedPart.FOOT) {
            BlockPos headPos = pos.relative(state.getValue(FACING));
            BlockState head = level.getBlockState(headPos);
            if (head.is(this) && head.getValue(PART) == BedPart.HEAD) {
                level.setBlock(headPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL | Block.UPDATE_SUPPRESS_DROPS);
                level.levelEvent(player, 2001, headPos, Block.getId(head));
            }
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }
}
