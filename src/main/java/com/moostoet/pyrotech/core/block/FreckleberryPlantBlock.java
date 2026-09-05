package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreBlocks;
import com.moostoet.pyrotech.core.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The freckleberry plant: a vanilla-style crop on farmland or mulched farmland. Ripe, it
 * hands over its berries on a right click and drops back one stage instead of being broken.
 */
public final class FreckleberryPlantBlock extends CropBlock implements BushSoil {

    private static final VoxelShape[] SHAPES = {
        Block.box(0, 0, 0, 16, 2, 16),
        Block.box(0, 0, 0, 16, 6, 16),
        Block.box(0, 0, 0, 16, 8, 16),
        Block.box(0, 0, 0, 16, 8, 16),
        Block.box(0, 0, 0, 16, 8, 16),
        Block.box(0, 0, 0, 16, 8, 16),
        Block.box(0, 0, 0, 16, 8, 16),
        Block.box(0, 0, 0, 16, 8, 16)
    };

    public FreckleberryPlantBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[this.getAge(state)];
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return CoreItems.FRECKLEBERRY_SEEDS.get();
    }

    /**
     * Farmland and mulched farmland. 1.12's seeds also accepted plain dirt, but the plant
     * could not stay on it and popped straight off, so the seeds no longer offer it.
     */
    @Override
    public boolean isValidSoil(LevelReader level, BlockPos soilPos, BlockState soil) {
        return soil.is(Blocks.FARMLAND) || soil.is(CoreBlocks.FARMLAND_MULCHED.get());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.is(Blocks.FARMLAND) || state.is(CoreBlocks.FARMLAND_MULCHED.get());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!this.isMaxAge(state)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (!level.isClientSide) {
            int fortune = EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolderOrThrow(Enchantments.FORTUNE),
                player.getMainHandItem());
            int count = 1 + level.random.nextInt(2 + fortune);
            popResource(level, pos, new ItemStack(CoreItems.FRECKLEBERRIES.get(), count));
            level.setBlock(pos, this.getStateForAge(this.getMaxAge() - 1), Block.UPDATE_ALL);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
