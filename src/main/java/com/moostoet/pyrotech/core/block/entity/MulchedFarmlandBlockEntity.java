package com.moostoet.pyrotech.core.block.entity;

import com.moostoet.pyrotech.core.CoreBlockEntities;
import com.moostoet.pyrotech.core.CoreConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Counts the bone meal charges a mulched farmland has left. Starts at the config value, never below one. */
public final class MulchedFarmlandBlockEntity extends BlockEntity {

    private int remainingCharges;

    public MulchedFarmlandBlockEntity(BlockPos pos, BlockState state) {
        super(CoreBlockEntities.FARMLAND_MULCHED.get(), pos, state);
        this.setRemainingCharges(CoreConfig.COMMON.mulchedFarmlandCharges.get());
    }

    public int getRemainingCharges() {
        return this.remainingCharges;
    }

    private void setRemainingCharges(int value) {
        this.remainingCharges = Math.max(1, value);
    }

    public void decrementRemainingCharges() {
        this.remainingCharges -= 1;
        this.setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("remainingCharges", this.remainingCharges);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.setRemainingCharges(tag.getInt("remainingCharges"));
    }
}
