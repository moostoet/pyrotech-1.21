package com.moostoet.pyrotech.core.entity;

import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.function.Supplier;

/**
 * A thrown rock, grass clump, or netherrack gib: a snowball that shows the rock it was
 * thrown as, does the 1.12 half heart on a hit, and bursts into that rock's block
 * particles. One class serves the three 1.12 entity ids; the item rides along in the
 * synced data.
 */
public final class ThrownRockEntity extends ThrowableItemProjectile {

    private static final float DAMAGE = 0.5F;

    private final Supplier<? extends Item> defaultItem;

    public ThrownRockEntity(EntityType<? extends ThrownRockEntity> type, Level level, Supplier<? extends Item> defaultItem) {
        super(type, level);
        this.defaultItem = defaultItem;
    }

    public ThrownRockEntity(EntityType<? extends ThrownRockEntity> type, LivingEntity shooter, Level level, Supplier<? extends Item> defaultItem) {
        super(type, shooter, level);
        this.defaultItem = defaultItem;
    }

    @Override
    protected Item getDefaultItem() {
        return this.defaultItem.get();
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 3 && this.getItem().getItem() instanceof BlockItem blockItem) {
            BlockState state = blockItem.getBlock().defaultBlockState();
            for (int i = 0; i < 8; i++) {
                this.level().addParticle(new BlockParticleOption(ParticleTypes.BLOCK, state), this.getX(), this.getY(), this.getZ(), 0, 0, 0);
            }
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(this.damageSources().thrown(this, this.getOwner()), DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!this.level().isClientSide) {
            this.level().broadcastEntityEvent(this, (byte) 3);
            this.discard();
        }
    }
}
