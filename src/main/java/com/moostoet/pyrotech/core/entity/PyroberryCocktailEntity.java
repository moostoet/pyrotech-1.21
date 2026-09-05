package com.moostoet.pyrotech.core.entity;

import com.moostoet.pyrotech.core.CoreItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * The thrown cocktail. It sets what it hits alight for twenty seconds, shatters with
 * glass, firework, and splash sounds, and lights a quarter of the open blocks that have
 * something under them within four blocks. The range, chance, and burn time are the 1.12
 * config defaults, now constants (core sign-off, item 3).
 */
public final class PyroberryCocktailEntity extends ThrowableItemProjectile {

    private static final int ENTITY_FIRE_SECONDS = 20;
    private static final int FIRE_RANGE = 4;
    private static final float FIRE_CHANCE = 0.25F;

    public PyroberryCocktailEntity(EntityType<? extends PyroberryCocktailEntity> type, Level level) {
        super(type, level);
    }

    public PyroberryCocktailEntity(EntityType<? extends PyroberryCocktailEntity> type, LivingEntity shooter, Level level) {
        super(type, shooter, level);
    }

    @Override
    protected Item getDefaultItem() {
        return CoreItems.PYROBERRY_COCKTAIL.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id != 3) {
            return;
        }
        ItemParticleOption shards = new ItemParticleOption(ParticleTypes.ITEM, this.getItem());
        RandomSource random = this.random;
        for (int i = 0; i < 8; i++) {
            this.level().addParticle(shards, this.getX(), this.getY(), this.getZ(),
                (random.nextFloat() - 0.5) * 0.08, (random.nextFloat() - 0.5) * 0.08, (random.nextFloat() - 0.5) * 0.08);
            this.level().addParticle(ParticleTypes.EXPLOSION, this.getX(), this.getY(), this.getZ(),
                (random.nextFloat() - 0.5) * 0.08, 0, (random.nextFloat() - 0.5) * 0.08);
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, random.nextFloat() * 0.5 + this.getX(), this.getY(), random.nextFloat() * 0.5 + this.getZ(),
                0, random.nextFloat() * 0.08, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().igniteForSeconds(ENTITY_FIRE_SECONDS);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (this.level().isClientSide) {
            return;
        }
        BlockPos pos = result instanceof BlockHitResult blockHit
            ? blockHit.getBlockPos()
            : ((EntityHitResult) result).getEntity().blockPosition();
        this.level().playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1, 1);
        this.level().playSound(null, pos, SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1, 1);
        this.level().playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.PLAYERS, 0.5F, 1);
        this.spreadFire(pos);
        this.level().broadcastEntityEvent(this, (byte) 3);
        this.discard();
    }

    /** The 1.12 range scan: every position in the cube whose centre distance is within the range. */
    private void spreadFire(BlockPos center) {
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-FIRE_RANGE, -FIRE_RANGE, -FIRE_RANGE),
            center.offset(FIRE_RANGE, FIRE_RANGE, FIRE_RANGE))) {
            if (pos.distSqr(center) > FIRE_RANGE * FIRE_RANGE) {
                continue;
            }
            if (this.level().getBlockState(pos).canBeReplaced()
                && !this.level().getBlockState(pos.below()).isAir()
                && this.random.nextFloat() < FIRE_CHANCE) {
                this.level().setBlockAndUpdate(pos, BaseFireBlock.getState(this.level(), pos));
            }
        }
    }
}
