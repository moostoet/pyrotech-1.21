package com.moostoet.pyrotech.core.block;

import com.moostoet.pyrotech.core.CoreConfig;
import com.moostoet.pyrotech.core.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * The gloamberry bush. Grows at night on dirt, loses its berries by day, takes bone meal
 * only after dark, and gives up a gloamberry to an empty hand when ripe. Shimmers when
 * touched. Growth chances come from config, as in 1.12.
 */
public final class GloamberryBushBlock extends BerryBushBlock {

    public GloamberryBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidSoil(LevelReader level, BlockPos soilPos, BlockState soil) {
        return soil.is(BlockTags.DIRT) && soil.isCollisionShapeFullBlock(level, soilPos);
    }

    // -- Interaction ---------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND || !stack.is(Items.BONE_MEAL)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        int age = age(state);
        if (!level.isDay() && age < MAX_AGE && CommonHooks.canCropGrow(level, pos, state, true)) {
            if (!player.isCreative() && !player.isSpectator()) {
                stack.shrink(1);
            }
            level.setBlock(pos, this.withAge(age + 1), Block.UPDATE_CLIENTS);
            level.levelEvent(2005, pos, 4);
            CommonHooks.fireCropGrowPost(level, pos, state);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        // 1.12 swallowed the click below max age and let it through at max age; neither does anything.
        return isMaxAge(state) ? ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION : ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty() || !isMaxAge(state)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            shimmer(serverLevel, pos);
            level.setBlock(pos, this.withAge(age(state) - 1), Block.UPDATE_CLIENTS);
            popResource(level, pos, new ItemStack(CoreItems.GLOAMBERRIES.get()));
            playSound(serverLevel, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel && isMaxAge(state)) {
            shimmer(serverLevel, pos);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel serverLevel && isMaxAge(state) && level.random.nextFloat() < 0.05) {
            shimmer(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel && isMaxAge(state)) {
            shimmer(serverLevel, pos);
            playSound(serverLevel, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    // -- Growth --------------------------------------------------------------

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.checkAndDrop(state, level, pos)) {
            return;
        }
        CoreConfig.Common config = CoreConfig.COMMON;
        if (level.isDay()) {
            if (isMaxAge(state) && random.nextFloat() < config.gloamberryDaytimeBerryLossChance.get()) {
                level.setBlock(pos, this.withAge(age(state) - 1), Block.UPDATE_CLIENTS);
            }
            return;
        }
        int age = age(state);
        boolean grew = false;
        if (age < MAX_AGE) {
            double chance = age == MAX_AGE - 1 ? config.gloamberryBerryGrowthChance.get() : config.gloamberryGrowthChance.get();
            if (!level.canSeeSky(pos)) {
                chance *= config.gloamberryObstructedGrowthModifier.get();
            }
            if (CommonHooks.canCropGrow(level, pos, state, random.nextFloat() < chance)) {
                level.setBlock(pos, this.withAge(age + 1), Block.UPDATE_CLIENTS);
                grew = true;
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
        if ((age > 2 && grew) || (isMaxAge(state) && random.nextFloat() < 0.5)) {
            shimmer(level, pos);
            playSound(level, pos);
        }
    }

    // -- Effects -------------------------------------------------------------

    private static void shimmer(ServerLevel level, BlockPos pos) {
        sendParticles(level, pos, ParticleTypes.EFFECT, 8);
    }

    private static void playSound(ServerLevel level, BlockPos pos) {
        if (level.random.nextFloat() < 0.25) {
            level.playSound(null, pos, SoundEvents.VEX_AMBIENT, SoundSource.BLOCKS, 0.75F, 1);
        }
    }
}
