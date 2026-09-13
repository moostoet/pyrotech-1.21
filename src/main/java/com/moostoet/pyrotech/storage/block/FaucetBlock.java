package com.moostoet.pyrotech.storage.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.block.entity.FaucetBlockEntity;
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
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.capabilities.Capabilities;

/**
 * The stone and refractory faucets: a spout on the face of a fluid handler that, while
 * active, moves {@code transferPerTick} millibuckets a tick from the block behind into the
 * block below, stopping when either side refuses or {@code transferLimit} has passed. Any
 * click toggles it; a redstone signal starts it. A stone faucet breaks on a hot fluid.
 */
public final class FaucetBlock extends BaseEntityBlock {

    public static final int NO_LIMIT = -1;

    public static final MapCodec<FaucetBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Codec.INT.fieldOf("transfer_per_tick").forGetter(FaucetBlock::transferPerTick),
        Codec.INT.fieldOf("transfer_limit").forGetter(FaucetBlock::transferLimit),
        Codec.BOOL.fieldOf("transfers_hot_fluids").forGetter(FaucetBlock::transfersHotFluids),
        propertiesCodec()
    ).apply(instance, FaucetBlock::new));

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape NORTH_SHAPE = Block.box(4, 4, 10, 12, 10, 16);
    private static final VoxelShape SOUTH_SHAPE = Block.box(4, 4, 0, 12, 10, 6);
    private static final VoxelShape EAST_SHAPE = Block.box(0, 4, 4, 6, 10, 12);
    private static final VoxelShape WEST_SHAPE = Block.box(10, 4, 4, 16, 10, 12);

    private final int transferPerTick;
    private final int transferLimit;
    private final boolean transfersHotFluids;

    public FaucetBlock(int transferPerTick, int transferLimit, boolean transfersHotFluids, Properties properties) {
        super(properties);
        this.transferPerTick = transferPerTick;
        this.transferLimit = transferLimit;
        this.transfersHotFluids = transfersHotFluids;
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public int transferPerTick() {
        return this.transferPerTick;
    }

    public int transferLimit() {
        return this.transferLimit;
    }

    public boolean transfersHotFluids() {
        return this.transfersHotFluids;
    }

    @Override
    protected MapCodec<FaucetBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    /** The spout points the way the clicked face does, so it hangs off the block it was placed against. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction face = context.getClickedFace();
        return this.defaultBlockState().setValue(FACING, face.getAxis().isHorizontal() ? face : Direction.NORTH);
    }

    /** A faucet needs a fluid handler behind it, on the face it hangs from. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        return !(level instanceof Level realLevel)
            || realLevel.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(facing.getOpposite()), facing) != null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case EAST -> EAST_SHAPE;
            case WEST -> WEST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    /** The 1.12 rule: the faucet drops when the block behind goes, and a signal switches it on. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        if (level.isEmptyBlock(pos.relative(state.getValue(FACING).getOpposite()))) {
            level.destroyBlock(pos, true);
            return;
        }
        if (level.hasNeighborSignal(pos) && level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet) {
            faucet.setActive(true);
        }
    }

    /** Any click toggles, a bucket included, as in 1.12. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        return this.toggle(level, pos) ? ItemInteractionResult.sidedSuccess(level.isClientSide) : ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return this.toggle(level, pos) ? InteractionResult.sidedSuccess(level.isClientSide) : InteractionResult.PASS;
    }

    private boolean toggle(Level level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof FaucetBlockEntity faucet)) {
            return false;
        }
        if (!level.isClientSide) {
            faucet.toggleActive();
        }
        return true;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FaucetBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || !StorageBlockEntities.is(type, StorageBlockEntities.FAUCET)) {
            return null;
        }
        return (tickLevel, pos, tickState, blockEntity) -> FaucetBlockEntity.serverTick(tickLevel, pos, tickState, (FaucetBlockEntity) blockEntity);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
