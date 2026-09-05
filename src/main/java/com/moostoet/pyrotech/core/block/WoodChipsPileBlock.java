package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** The pile of wood chips. Each level is one rock of wood chips; a shovel is needed while the tweak is on. */
public final class WoodChipsPileBlock extends PileBlock {

    public WoodChipsPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getDrop(Level level, BlockPos pos, BlockState state) {
        return new ItemStack(CoreBlocks.ROCK_WOOD_CHIPS.get());
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        if (CoreConfig.COMMON.requireShovelToPickupWoodChips.get()) {
            return player.getMainHandItem().canPerformAction(ItemAbilities.SHOVEL_DIG);
        }
        return super.canHarvestBlock(state, level, pos, player);
    }
}
