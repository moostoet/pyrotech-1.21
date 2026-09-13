package com.moostoet.pyrotech.hunting.block;

import com.mojang.serialization.MapCodec;
import com.moostoet.pyrotech.hunting.block.entity.Butchering;
import com.moostoet.pyrotech.hunting.block.entity.ButchersBlockBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The butcher's block: a carcass item goes in from the top and a knife works it there, with
 * the knife in hand choosing the butchering transform. The 1.12 interaction order stands:
 * the input slot first, then the knife loop.
 */
public final class ButchersBlock extends BaseEntityBlock {

    public static final MapCodec<ButchersBlock> CODEC = simpleCodec(ButchersBlock::new);

    public ButchersBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<ButchersBlock> codec() {
        return CODEC;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                             InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ButchersBlockBlockEntity block)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hand == InteractionHand.MAIN_HAND && block.insert(player, stack, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return Butchering.use(block, player, hand, stack, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof ButchersBlockBlockEntity block)) {
            return InteractionResult.PASS;
        }
        if (block.extract(player, hit)) {
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return Butchering.use(block, player, InteractionHand.MAIN_HAND, ItemStack.EMPTY, hit).result();
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof ButchersBlockBlockEntity block) {
            block.dropContents();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ButchersBlockBlockEntity(pos, state);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
