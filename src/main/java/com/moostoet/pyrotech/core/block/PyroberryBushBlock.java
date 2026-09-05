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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.common.CommonHooks;
import net.neoforged.neoforge.common.ItemAbilities;

/**
 * The pyroberry bush. Grows on sand by day under open sky, is beaten back by rain and by
 * cover, takes blaze powder, and explodes when ripe and provoked. Shears take the berries
 * safely; that was 1.12's {@code IBlockShearable} handler, now the bush's own item hook.
 */
public final class PyroberryBushBlock extends BerryBushBlock {

    private static final float EXPLOSION_RADIUS = 1.5F;

    public PyroberryBushBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isValidSoil(LevelReader level, BlockPos soilPos, BlockState soil) {
        return soil.is(BlockTags.SAND);
    }

    // -- Interaction ---------------------------------------------------------

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (hand != InteractionHand.MAIN_HAND) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        if (stack.canPerformAction(ItemAbilities.SHEARS_HARVEST)) {
            if (!isMaxAge(state)) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (level instanceof ServerLevel serverLevel) {
                level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1, 1);
                level.setBlock(pos, this.withAge(MAX_AGE - 1), Block.UPDATE_ALL);
                stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                burst(serverLevel, pos);
                popResource(level, pos, new ItemStack(CoreItems.PYROBERRIES.get(), level.random.nextInt(3) + 1));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (stack.is(Items.BLAZE_POWDER)) {
            int age = age(state);
            if (age < MAX_AGE && CommonHooks.canCropGrow(level, pos, state, true)) {
                if (!player.isCreative() && !player.isSpectator()) {
                    stack.shrink(1);
                }
                level.setBlock(pos, this.withAge(age + 1), Block.UPDATE_CLIENTS);
                if (level instanceof ServerLevel serverLevel) {
                    burst(serverLevel, pos);
                }
                CommonHooks.fireCropGrowPost(level, pos, state);
            } else if (isMaxAge(state) && level instanceof ServerLevel serverLevel) {
                burst(serverLevel, pos);
                explode(level, pos);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!player.getMainHandItem().isEmpty() || !isMaxAge(state)) {
            return super.useWithoutItem(state, level, pos, player, hitResult);
        }
        if (level instanceof ServerLevel serverLevel) {
            burst(serverLevel, pos);
            explode(level, pos);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel serverLevel && isMaxAge(state)) {
            burst(serverLevel, pos);
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel serverLevel && isMaxAge(state) && level.random.nextFloat() < 0.05) {
            burst(serverLevel, pos);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level instanceof ServerLevel serverLevel) {
            if (!isSoft(state)) {
                burst(serverLevel, pos);
            }
            if (isMaxAge(state)) {
                explode(level, pos);
            }
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
        int age = age(state);
        if (!level.canSeeSky(pos)) {
            if (age > 4 && CommonHooks.canCropGrow(level, pos, state, random.nextFloat() < config.pyroberryObstructedGrowthRevertChance.get())) {
                level.setBlock(pos, this.withAge(age - 1), Block.UPDATE_CLIENTS);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
            return;
        }
        if (level.isRainingAt(pos.above())) {
            if (age > 4 && CommonHooks.canCropGrow(level, pos, state, random.nextFloat() < config.pyroberryRainGrowthRevertChance.get())) {
                level.setBlock(pos, this.withAge(age - 1), Block.UPDATE_CLIENTS);
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
            for (int i = 0; i < 8; i++) {
                double x = pos.getX() + 0.5 + (random.nextDouble() * 2 - 1) * 0.4;
                double y = pos.getY() + 0.6 + (random.nextDouble() * 2 - 1) * 0.4;
                double z = pos.getZ() + 0.5 + (random.nextDouble() * 2 - 1) * 0.4;
                level.sendParticles(random.nextFloat() < 0.25 ? ParticleTypes.LARGE_SMOKE : ParticleTypes.SMOKE, x, y, z, 4, 0, 0, 0, 0);
            }
            level.playSound(null, pos, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1);
            return;
        }
        if (!level.isDay()) {
            return;
        }
        boolean grew = false;
        if (age < MAX_AGE) {
            double chance = age == MAX_AGE - 1 ? config.pyroberryBerryGrowthChance.get() : config.pyroberryGrowthChance.get();
            if (CommonHooks.canCropGrow(level, pos, state, random.nextFloat() < chance)) {
                level.setBlock(pos, this.withAge(age + 1), Block.UPDATE_CLIENTS);
                grew = true;
                CommonHooks.fireCropGrowPost(level, pos, state);
            }
        }
        if ((age > 2 && grew) || (isMaxAge(state) && random.nextFloat() < 0.5)) {
            burst(level, pos);
        }
    }

    // -- Effects -------------------------------------------------------------

    /** The 1.12 combust packet plus the fire sound that always went with it. */
    private static void burst(ServerLevel level, BlockPos pos) {
        sendParticles(level, pos, ParticleTypes.SMOKE, 16);
        sendParticles(level, pos, ParticleTypes.LARGE_SMOKE, 4);
        sendParticles(level, pos, ParticleTypes.FLAME, 16);
        level.playSound(null, pos, SoundEvents.FIRE_AMBIENT, SoundSource.BLOCKS, 1, 1);
    }

    private static void explode(Level level, BlockPos pos) {
        level.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, EXPLOSION_RADIUS, true, Level.ExplosionInteraction.BLOCK);
    }
}
