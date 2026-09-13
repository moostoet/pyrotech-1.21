package com.moostoet.pyrotech.hunting.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.hunting.HuntingBlocks;
import com.moostoet.pyrotech.hunting.block.entity.Butchering;
import com.moostoet.pyrotech.hunting.block.entity.CarcassBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * The carcass: the block a Pyrotech-killed animal leaves, holding its drops until a knife
 * works them out. Its contents ride vanilla's container component on the item, so the
 * loot table, pick-block, and placement carry them (hunting porting notes, carcass).
 */
public final class CarcassBlock extends BaseEntityBlock {

    public static final MapCodec<CarcassBlock> CODEC = simpleCodec(CarcassBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH_SOUTH = Block.box(3, 0, 0, 13, 10, 16);
    private static final VoxelShape SHAPE_EAST_WEST = Block.box(0, 0, 3, 16, 10, 13);

    public CarcassBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** A carcass item holding these stacks, as a kill leaves it. */
    public static ItemStack withContents(List<ItemStack> contents) {
        ItemStack stack = new ItemStack(HuntingBlocks.CARCASS_ITEM.get());
        stack.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        return stack;
    }

    @Override
    protected MapCodec<CarcassBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return this.defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NORTH_SOUTH : SHAPE_EAST_WEST;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CarcassBlockEntity carcass) {
            return Butchering.use(carcass, player, hand, stack, hit);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof CarcassBlockEntity carcass) {
            return Butchering.use(carcass, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit).result();
        }
        return InteractionResult.PASS;
    }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = super.getCloneItemStack(level, pos, state);
        if (level.getBlockEntity(pos) instanceof CarcassBlockEntity carcass) {
            carcass.saveToItem(stack, level.registryAccess());
        }
        return stack;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CarcassBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
