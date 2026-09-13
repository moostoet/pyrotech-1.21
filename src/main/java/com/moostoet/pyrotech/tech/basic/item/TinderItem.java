package com.moostoet.pyrotech.tech.basic.item;

import com.moostoet.pyrotech.tech.basic.TechBasicBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Tinder: used against a block, it lays an unlit campfire on the clicked face. */
public final class TinderItem extends Item {

    public TinderItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() != HitResult.Type.BLOCK) {
            return InteractionResultHolder.pass(stack);
        }
        BlockPos pos = hit.getBlockPos();
        if (!level.getBlockState(pos).canBeReplaced()) {
            pos = pos.relative(hit.getDirection());
        }
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResultHolder.fail(stack);
        }
        BlockState campfire = TechBasicBlocks.CAMPFIRE.get().defaultBlockState();
        if (!level.getBlockState(pos).canBeReplaced() || !campfire.canSurvive(level, pos)) {
            return InteractionResultHolder.fail(stack);
        }
        if (!level.isClientSide) {
            level.setBlock(pos, campfire, Block.UPDATE_ALL);
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.GRASS_PLACE, SoundSource.BLOCKS, 1, 1);
            if (!player.isCreative()) {
                stack.shrink(1);
            }
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
