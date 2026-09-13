package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.core.CombustParticles;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.fluid.HotFluidTank;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.block.FaucetBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A faucet's state: whether it runs, how much it has moved since it was switched on, and
 * the fluid it shows, which is the last fluid it moved. Only {@code active} and the shown
 * fluid sync, and the shown fluid only when its kind changes (storage sign-off, item 7).
 */
public final class FaucetBlockEntity extends SyncedBlockEntity {

    private boolean active;
    private int transferred;
    private FluidStack displayed = FluidStack.EMPTY;

    public FaucetBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.FAUCET.get(), pos, state);
    }

    public FaucetBlock block() {
        return (FaucetBlock) this.getBlockState().getBlock();
    }

    public Direction facing() {
        return this.getBlockState().getValue(FaucetBlock.FACING);
    }

    public boolean isActive() {
        return this.active;
    }

    public FluidStack displayed() {
        return this.displayed;
    }

    public void toggleActive() {
        this.setActive(!this.active);
    }

    /** Switching on, or off, or on again restarts the count toward the limit. */
    public void setActive(boolean active) {
        this.transferred = 0;
        if (this.active != active) {
            this.active = active;
            this.sync();
        }
    }

    /** The 1.12 tick: probe both sides, show the fluid, then move it. */
    public static void serverTick(Level level, BlockPos pos, BlockState state, FaucetBlockEntity faucet) {
        if (!faucet.active) {
            return;
        }
        FaucetBlock block = faucet.block();
        if (block.transferLimit() != FaucetBlock.NO_LIMIT && faucet.transferred >= block.transferLimit()) {
            faucet.setActive(false);
            return;
        }
        Direction facing = state.getValue(FaucetBlock.FACING);
        IFluidHandler source = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.relative(facing.getOpposite()), facing);
        IFluidHandler target = level.getCapability(Capabilities.FluidHandler.BLOCK, pos.below(), Direction.UP);
        if (source == null || target == null) {
            faucet.setActive(false);
            return;
        }
        FluidStack drained = source.drain(block.transferPerTick(), IFluidHandler.FluidAction.SIMULATE);
        if (drained.isEmpty()) {
            faucet.setActive(false);
            return;
        }
        int filled = target.fill(drained, IFluidHandler.FluidAction.SIMULATE);
        if (filled == 0) {
            faucet.setActive(false);
            return;
        }
        if (!block.transfersHotFluids() && HotFluidTank.isHot(drained)) {
            faucet.breakOnHotFluid();
        }
        if (!FluidStack.isSameFluidSameComponents(faucet.displayed, drained)) {
            faucet.displayed = drained.copyWithAmount(FluidType.BUCKET_VOLUME);
            faucet.sync();
        }
        faucet.transferred += filled;
        target.fill(source.drain(filled, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
    }

    /** A hot fluid through a stone faucet: the faucet breaks and smokes, and the fluid still passes once. */
    private void breakOnHotFluid() {
        if (!(this.level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.removeBlock(this.worldPosition, false);
        serverLevel.playSound(null, this.worldPosition, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 1, 1);
        CombustParticles.spawn(serverLevel, this.worldPosition, 0.2);
    }

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("active", this.active);
        if (!this.displayed.isEmpty()) {
            tag.put("displayed", this.displayed.save(registries));
        }
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.active = tag.getBoolean("active");
        this.displayed = FluidStack.parseOptional(registries, tag.getCompound("displayed"));
    }
}
