package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.block.BushSoil;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Supplier;

/**
 * Seeds for one of the berry plants. Clicking the top of soil the plant accepts, with air
 * above, plants it. The 1.12 {@code ItemBushSeedsBase}.
 */
public final class BushSeedsItem extends Item {

    private final Supplier<? extends Block> plant;

    public <B extends Block & BushSoil> BushSeedsItem(Supplier<B> plant, Properties properties) {
        super(properties);
        this.plant = plant;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos soilPos = context.getClickedPos();
        BlockPos plantPos = soilPos.above();
        Player player = context.getPlayer();
        ItemStack stack = context.getItemInHand();
        Block plant = this.plant.get();
        BlockState soil = level.getBlockState(soilPos);
        if (context.getClickedFace() != Direction.UP
            || (player != null && !player.mayUseItemAt(plantPos, Direction.UP, stack))
            || !((BushSoil) plant).isValidSoil(level, soilPos, soil)
            || !level.isEmptyBlock(plantPos)) {
            return InteractionResult.FAIL;
        }
        level.setBlock(plantPos, plant.defaultBlockState(), Block.UPDATE_ALL);
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, plantPos, stack);
        }
        stack.consume(1, player);
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
