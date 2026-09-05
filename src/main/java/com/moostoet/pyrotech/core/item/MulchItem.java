package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Mulch turns farmland into mulched farmland on a right click. Clicking the plant on top
 * of the farmland counts too. The moisture rule is the config's.
 */
public final class MulchItem extends Item {

    public MulchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);
        if (!state.is(Blocks.FARMLAND)) {
            pos = pos.below();
            state = level.getBlockState(pos);
        }
        Player player = context.getPlayer();
        Direction face = context.getClickedFace();
        ItemStack stack = context.getItemInHand();
        if (player != null && (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos.relative(face), face, stack))) {
            return InteractionResult.PASS;
        }
        if (!canMulch(state)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide) {
            level.setBlockAndUpdate(pos, CoreBlocks.FARMLAND_MULCHED.get().defaultBlockState());
            BlockPos soundPos = player != null ? player.blockPosition() : pos;
            level.playSound(null, soundPos, SoundEvents.GRASS_PLACE, SoundSource.PLAYERS, 1, 0.9F + level.getRandom().nextFloat() * 0.15F);
        }
        stack.consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    public static boolean canMulch(BlockState state) {
        if (!state.is(Blocks.FARMLAND)) {
            return false;
        }
        return !CoreConfig.COMMON.mulchedFarmlandRestrictToMoisturized.get() || state.getValue(FarmBlock.MOISTURE) > 0;
    }
}
