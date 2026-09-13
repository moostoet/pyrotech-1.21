package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.tech.basic.TechBasicBlockEntities;
import com.moostoet.pyrotech.tech.basic.TechBasicItems;
import com.moostoet.pyrotech.tech.basic.block.entity.BarrelBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/**
 * The barrel: a bucket of fluid and up to four items in a two by two grid on top, then
 * the lid seals it and the recipe runs. The 1.12 pair of blocks is one block with a
 * {@code sealed} property (tech/basic sign-off, item 9).
 */
public final class BarrelBlock extends BaseEntityBlock {

    public static final MapCodec<BarrelBlock> CODEC = simpleCodec(BarrelBlock::new);
    public static final BooleanProperty SEALED = BlockStateProperties.OPEN;

    public BarrelBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SEALED, false));
    }

    @Override
    protected MapCodec<BarrelBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SEALED);
    }

    /** On top, the 1.12 order: a fluid container, the lid, then the grid cell under the cursor. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) || barrel.isSealed()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (FluidUtil.getFluidHandler(stack).isPresent()) {
            if (!level.isClientSide) {
                FluidUtil.interactWithFluidHandler(player, hand, level, pos, Direction.UP);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(TechBasicItems.BARREL_LID.get())) {
            return barrel.insertLid(player, stack)
                ? ItemInteractionResult.sidedSuccess(level.isClientSide)
                : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (barrel.insertItem(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP || !(level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel)) {
            return InteractionResult.PASS;
        }
        if (barrel.isSealed() ? barrel.extractLid(player) : barrel.extractItem(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Pick-block hands out a sealed barrel with its contents, as the loot table does. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (state.getValue(SEALED) && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            stack.applyComponents(barrel.collectComponents());
        }
        return stack;
    }

    /** An unsealed barrel spills what it holds; a sealed one carries it in its item. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !state.getValue(SEALED) && level.getBlockEntity(pos) instanceof BarrelBlockEntity barrel) {
            barrel.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BarrelBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide
            ? createTickerHelper(type, TechBasicBlockEntities.BARREL.get(), BarrelBlockEntity::clientTick)
            : createTickerHelper(type, TechBasicBlockEntities.BARREL.get(), BarrelBlockEntity::serverTick);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
