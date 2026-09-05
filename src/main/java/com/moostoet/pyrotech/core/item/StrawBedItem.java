package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.block.StrawBedBlock;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BedPart;

/**
 * Places the straw bed the 1.12 way: on the top face of a block only, foot where clicked
 * (or above it when the clicked block is not replaceable) and head one block in the
 * player's facing, both on solid ground.
 */
public final class StrawBedItem extends BlockItem {

    public StrawBedItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return InteractionResult.FAIL;
        }
        BlockPos footPos = context.getClickedPos();
        if (!level.getBlockState(footPos).canBeReplaced()) {
            footPos = footPos.above();
        }
        Direction facing = context.getHorizontalDirection();
        BlockPos headPos = footPos.relative(facing);
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        if (player != null && !(player.mayUseItemAt(footPos, Direction.UP, stack) && player.mayUseItemAt(headPos, Direction.UP, stack))) {
            return InteractionResult.FAIL;
        }
        if (!canPlaceAt(level, footPos) || !canPlaceAt(level, headPos)) {
            return InteractionResult.FAIL;
        }
        BlockState foot = this.getBlock().defaultBlockState()
            .setValue(StrawBedBlock.OCCUPIED, false)
            .setValue(StrawBedBlock.FACING, facing)
            .setValue(StrawBedBlock.PART, BedPart.FOOT);
        level.setBlock(footPos, foot, Block.UPDATE_ALL);
        level.setBlock(headPos, foot.setValue(StrawBedBlock.PART, BedPart.HEAD), Block.UPDATE_ALL);
        SoundType sound = foot.getSoundType(level, footPos, player);
        level.playSound(null, footPos, sound.getPlaceSound(), SoundSource.BLOCKS, (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, footPos, stack);
        }
        stack.consume(1, player);
        return InteractionResult.CONSUME;
    }

    private static boolean canPlaceAt(Level level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(pos).canBeReplaced() && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
