package com.moostoet.pyrotech.tech.basic.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.tech.basic.block.entity.ChoppingBlockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * The chopping block: a log on top, an axe to split it into planks. Every sixteen chops
 * wear it a stage; the sixth stage breaks it. Chips pile up on it and scatter around it.
 */
public final class ChoppingBlockBlock extends BaseEntityBlock {

    public static final MapCodec<ChoppingBlockBlock> CODEC = simpleCodec(ChoppingBlockBlock::new);
    public static final int MAX_DAMAGE = 5;
    public static final IntegerProperty DAMAGE = IntegerProperty.create("damage", 0, MAX_DAMAGE);
    public static final IntegerProperty SAWDUST = IntegerProperty.create("sawdust", 0, ChoppingBlockBlockEntity.MAX_SAWDUST);

    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 6, 16);

    public ChoppingBlockBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(DAMAGE, 0).setValue(SAWDUST, 0));
    }

    @Override
    protected MapCodec<ChoppingBlockBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DAMAGE, SAWDUST);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    /** The 1.12 order: an axe chops, a shovel scoops, anything else goes on top. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChoppingBlockBlockEntity block)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hit.getDirection() == Direction.UP && stack.canPerformAction(ItemAbilities.AXE_DIG) && block.chop(player, stack, hand, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.canPerformAction(ItemAbilities.SHOVEL_DIG) && block.scoopSawdust(player, stack, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() == Direction.UP && block.insert(player, stack)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ChoppingBlockBlockEntity block)) {
            return InteractionResult.PASS;
        }
        if (hit.getDirection() == Direction.UP && block.extract(player)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (block.scoopSawdust(player, ItemStack.EMPTY, InteractionHand.MAIN_HAND)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return InteractionResult.PASS;
    }

    /** Pick-block hands out the block at its wear stage, as the loot table does. */
    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return WearStage.with(super.getCloneItemStack(level, pos, state), DAMAGE, state.getValue(DAMAGE));
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ChoppingBlockBlockEntity block) {
            block.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ChoppingBlockBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
