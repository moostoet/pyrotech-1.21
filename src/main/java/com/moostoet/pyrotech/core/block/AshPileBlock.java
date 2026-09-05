package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreItems;
import com.moostoet.pyrotech.core.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** The ash pile a burn leaves. A shovel takes it apart into pit ash, one level at a time. */
public final class AshPileBlock extends PileBlock {

    public AshPileBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemStack getDrop(Level level, BlockPos pos, BlockState state) {
        return new ItemStack(CoreItems.material(Material.PIT_ASH).get());
    }

    @Override
    public boolean canHarvestBlock(BlockState state, BlockGetter level, BlockPos pos, Player player) {
        return player.getMainHandItem().canPerformAction(ItemAbilities.SHOVEL_DIG);
    }
}
