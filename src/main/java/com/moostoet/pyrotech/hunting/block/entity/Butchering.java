package com.moostoet.pyrotech.hunting.block.entity;

import com.moostoet.pyrotech.core.PyrotechTags;
import com.moostoet.pyrotech.core.network.NoHungerPayload;
import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import com.moostoet.pyrotech.hunting.HuntingBlocks;
import com.moostoet.pyrotech.hunting.HuntingDataMaps;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The knife loop the carcass and the butcher's block share: the 1.12 {@code InteractionCarcass}
 * over its delegate. A main-hand use needs three hunger, and a knife; each use costs the knife
 * one durability and the player the target's exhaustion, adds the knife's efficiency to the
 * progress, and at the end of a cycle drops one item on top, then resets or destroys the target.
 */
public final class Butchering {

    public static final int MINIMUM_HUNGER = 3;
    private static final int TOTAL_PROGRESS = 100;
    private static final int PROGRESS_PARTICLES = 2;
    private static final int CRACK_PARTICLES = 8;
    private static final RandomSource RANDOM = RandomSource.create();

    private Butchering() {
    }

    /** What the loop asks of a carcass or a butcher's block. */
    public interface Target {

        Level level();

        BlockPos pos();

        /** False on a butcher's block with no carcass in it. */
        boolean hasWork();

        float exhaustion();

        boolean atButchersBlock();

        float progress();

        void setProgress(float progress);

        void resetProgress();

        /** One item out, transformed as the knife dictates. */
        ItemStack extract(ItemStack knife);

        boolean isEmpty();

        void destroy();

        double particleOffsetY();
    }

    /** The 1.12 total, plus or minus ten percent, at least one. */
    public static float randomProgress() {
        float adjustment = RANDOM.nextFloat() * 0.2f - 0.1f;
        return Math.max(1, TOTAL_PROGRESS + TOTAL_PROGRESS * adjustment);
    }

    public static ItemInteractionResult use(Target target, Player player, InteractionHand hand, ItemStack held, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND || !target.hasWork()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        Level level = target.level();
        if (player.getFoodData().getFoodLevel() < MINIMUM_HUNGER) {
            if (player instanceof ServerPlayer serverPlayer) {
                NoHungerPayload.send(serverPlayer);
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }
        if (!held.is(PyrotechTags.Items.KNIVES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        BlockPos pos = target.pos();
        if (level instanceof ServerLevel serverLevel) {
            if (target.exhaustion() > 0) {
                player.causeFoodExhaustion(target.exhaustion());
            }
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SLIME_BLOCK_PLACE, SoundSource.BLOCKS,
                0.75f, (float) (1 + level.random.nextGaussian() * 0.4));
            target.setProgress(target.progress() - HuntingDataMaps.efficiency(held, target.atButchersBlock()));
            ProgressParticlesPayload.send(serverLevel, pos.getX() + 0.5, pos.getY() + target.particleOffsetY(), pos.getZ() + 0.5,
                PROGRESS_PARTICLES);
            if (target.progress() <= 0) {
                ItemStack extracted = target.extract(held);
                if (!extracted.isEmpty()) {
                    Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, extracted);
                }
                if (target.isEmpty()) {
                    target.destroy();
                } else {
                    target.resetProgress();
                }
            }
            held.hurtAndBreak(1, player, LivingEntity.getSlotForHand(hand));
        } else {
            BlockState carcass = HuntingBlocks.CARCASS.get().defaultBlockState();
            Vec3 at = hit.getLocation();
            for (int i = 0; i < CRACK_PARTICLES; i++) {
                level.addParticle(new BlockParticleOption(ParticleTypes.BLOCK, carcass), at.x, at.y, at.z, 0, 0, 0);
            }
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
}
