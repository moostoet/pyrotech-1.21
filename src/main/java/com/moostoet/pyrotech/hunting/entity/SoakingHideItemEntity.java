package com.moostoet.pyrotech.hunting.entity;

import com.moostoet.pyrotech.core.network.ProgressParticlesPayload;
import com.moostoet.pyrotech.hunting.HuntingComponents;
import com.moostoet.pyrotech.hunting.HuntingEntities;
import com.moostoet.pyrotech.hunting.item.ScrapedHideItem;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A dropped scraped hide: an item entity that never expires and counts the ticks it lies in
 * water, becoming the washed hide after ten minutes. Every ten seconds of soaking it writes
 * its count into the stack, so a hide picked up and dropped again carries on.
 */
public final class SoakingHideItemEntity extends ItemEntity {

    public static final int SOAK_TICKS = 20 * 60 * 10;
    private static final int PARTICLE_INTERVAL = 40;
    private static final int SAVE_INTERVAL = 200;

    private int ticksInWater;

    public SoakingHideItemEntity(EntityType<? extends ItemEntity> type, Level level) {
        super(type, level);
        this.lifespan = Integer.MAX_VALUE;
    }

    public SoakingHideItemEntity(Level level, double x, double y, double z, ItemStack stack) {
        this(HuntingEntities.HIDE_SCRAPED_ITEM.get(), level);
        this.setPos(x, y, z);
        this.setItem(stack);
        this.ticksInWater = stack.getOrDefault(HuntingComponents.SOAK_TICKS, 0);
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.level() instanceof ServerLevel serverLevel) || this.isRemoved()) {
            return;
        }
        ItemStack stack = this.getItem();
        Item washed = ScrapedHideItem.washedFor(stack);
        if (washed == null) {
            return;
        }
        BlockPos pos = this.blockPosition();
        if (serverLevel.getFluidState(pos).is(FluidTags.WATER)) {
            this.ticksInWater++;
            if (this.ticksInWater % PARTICLE_INTERVAL == 0) {
                ProgressParticlesPayload.send(serverLevel, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 2);
            }
            if (this.ticksInWater % SAVE_INTERVAL == 0) {
                stack.set(HuntingComponents.SOAK_TICKS, this.ticksInWater);
            }
        }
        if (this.ticksInWater >= SOAK_TICKS) {
            this.setItem(new ItemStack(washed, stack.getCount()));
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ticksInWater", this.ticksInWater);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.ticksInWater = tag.getInt("ticksInWater");
    }
}
