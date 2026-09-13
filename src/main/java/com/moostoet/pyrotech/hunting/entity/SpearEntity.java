package com.moostoet.pyrotech.hunting.entity;

import com.moostoet.pyrotech.hunting.HuntingEntities;
import com.moostoet.pyrotech.hunting.HuntingItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * A thrown spear: an arrow carrying its item, which sticks in a living target, drops as an
 * item off anything else, and lies in the ground to be picked up. The thrown stack is also
 * the weapon, so Power, Punch, and Flame on it act through vanilla's paths.
 */
public final class SpearEntity extends AbstractArrow {

    private static final EntityDataAccessor<ItemStack> DATA_ITEM = SynchedEntityData.defineId(SpearEntity.class, EntityDataSerializers.ITEM_STACK);

    public SpearEntity(EntityType<? extends SpearEntity> type, Level level) {
        super(type, level);
    }

    public SpearEntity(Level level, LivingEntity thrower, ItemStack spear) {
        super(HuntingEntities.SPEAR.get(), thrower, level, spear.copyWithCount(1), spear);
        this.entityData.set(DATA_ITEM, spear.copyWithCount(1));
    }

    /** The spear as the client sees it, for the renderer. */
    public ItemStack getItem() {
        return this.entityData.get(DATA_ITEM);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ITEM, ItemStack.EMPTY);
    }

    @Override
    protected ItemStack getDefaultPickupItem() {
        return new ItemStack(HuntingItems.CRUDE_SPEAR.get());
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        Entity hit = result.getEntity();
        if (!this.level().isClientSide) {
            ItemStack spear = this.getPickupItemStackOrigin().copy();
            if (hit instanceof LivingEntity living && !living.isInvulnerable()) {
                StuckSpears.add(living, spear);
            } else {
                this.spawnAtLocation(spear, 0);
            }
            this.pickup = Pickup.DISALLOWED;
        }
        super.onHitEntity(result);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(DATA_ITEM, this.getPickupItemStackOrigin().copy());
    }
}
