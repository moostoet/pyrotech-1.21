package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.block.entity.DryingRackBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The crude drying rack, one slot on a wall bracket, and the drying rack, four slots on
 * a frame that can stack into a ladder. Both dry by the weather: sun and fire speed
 * them, rain stops or undoes them.
 */
public final class DryingRackBlock extends BaseEntityBlock {

    public static final MapCodec<DryingRackBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.BOOL.fieldOf("crude").forGetter(DryingRackBlock::isCrude),
        propertiesCodec()
    ).apply(instance, DryingRackBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** A drying rack with another drying rack above it: the ladder and the stacked model. */
    public static final BooleanProperty STACKED = BooleanProperty.create("stacked");

    private static final int FLAMMABILITY = 150;
    private static final VoxelShape CRUDE_SOUTH = Block.box(0, 11, 11, 16, 16, 16);
    private static final VoxelShape CRUDE_NORTH = Block.box(0, 11, 0, 16, 16, 5);
    private static final VoxelShape CRUDE_EAST = Block.box(11, 11, 0, 16, 16, 16);
    private static final VoxelShape CRUDE_WEST = Block.box(0, 11, 0, 5, 16, 16);
    private static final VoxelShape NORMAL = Block.box(1, 11, 1, 15, 12, 15);

    private final boolean crude;

    public DryingRackBlock(boolean crude, Properties properties) {
        super(properties);
        this.crude = crude;
        BlockState state = this.stateDefinition.any().setValue(FACING, Direction.NORTH);
        this.registerDefaultState(crude ? state : state.setValue(STACKED, false));
    }

    public boolean isCrude() {
        return this.crude;
    }

    public int slots() {
        return this.crude ? 1 : DryingRackBlockEntity.NORMAL_SLOTS;
    }

    @Override
    protected MapCodec<DryingRackBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
        if (!this.crude) {
            builder.add(STACKED);
        }
    }

    /** The 1.12 placement: against the clicked wall, or the way the player faces; against a crude rack, its way. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing = clicked.getAxis().isHorizontal() ? clicked.getOpposite() : context.getHorizontalDirection();
        BlockState against = context.getLevel().getBlockState(context.getClickedPos().relative(clicked.getOpposite()));
        if (against.getBlock() instanceof DryingRackBlock rack && rack.crude) {
            facing = against.getValue(FACING);
        }
        BlockState state = this.defaultBlockState().setValue(FACING, facing);
        return this.crude ? state : state.setValue(STACKED, isNormalRack(context.getLevel().getBlockState(context.getClickedPos().above())));
    }

    private static boolean isNormalRack(BlockState state) {
        return state.getBlock() instanceof DryingRackBlock rack && !rack.crude;
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (!this.crude && direction == Direction.UP) {
            return state.setValue(STACKED, isNormalRack(neighborState));
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!this.crude) {
            return NORMAL;
        }
        return switch (state.getValue(FACING)) {
            case SOUTH -> CRUDE_SOUTH;
            case EAST -> CRUDE_EAST;
            case WEST -> CRUDE_WEST;
            default -> CRUDE_NORTH;
        };
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return FLAMMABILITY;
    }

    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return true;
    }

    // -- Interaction ---------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && rack.insert(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack && rack.extract(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof DryingRackBlockEntity rack) {
            rack.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DryingRackBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
            ? createTickerHelper(type, TechBasicBlockEntities.DRYING_RACK.get(), DryingRackBlockEntity::clientTick)
            : createTickerHelper(type, TechBasicBlockEntities.DRYING_RACK.get(), DryingRackBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
