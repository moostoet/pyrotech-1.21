package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.block.entity.CompostBinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The compost bin: plant matter and water in, mulch out, a layer at a time. The
 * {@code state} property is dry, wet, or ready; {@code compost_value} is the fill in
 * fifths, for the model.
 */
public final class CompostBinBlock extends BaseEntityBlock {

    public static final MapCodec<CompostBinBlock> CODEC = simpleCodec(CompostBinBlock::new);
    public static final int DRY = 0;
    public static final int WET = 1;
    public static final int READY = 2;
    public static final IntegerProperty STATE = IntegerProperty.create("state", DRY, READY);
    public static final IntegerProperty COMPOST_VALUE = IntegerProperty.create("compost_value", 0, 5);

    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 16, 15);

    public CompostBinBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STATE, DRY).setValue(COMPOST_VALUE, 0));
    }

    @Override
    protected MapCodec<CompostBinBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STATE, COMPOST_VALUE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** A bucket waters, a shovel takes mulch, and anything else goes in. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof CompostBinBlockEntity bin)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (FluidUtil.getFluidHandler(stack).isPresent()) {
            if (!level.isClientSide) {
                FluidUtil.interactWithFluidHandler(player, hand, level, pos, Direction.UP);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG) && bin.takeOutput(player, stack, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (bin.insert(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() == Direction.UP && level.getBlockEntity(pos) instanceof CompostBinBlockEntity bin && bin.extract(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof CompostBinBlockEntity bin) {
            bin.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CompostBinBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
            ? createTickerHelper(type, TechBasicBlockEntities.COMPOST_BIN.get(), CompostBinBlockEntity::clientTick)
            : createTickerHelper(type, TechBasicBlockEntities.COMPOST_BIN.get(), CompostBinBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
