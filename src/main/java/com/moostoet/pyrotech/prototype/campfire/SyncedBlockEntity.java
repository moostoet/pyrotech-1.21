package com.moostoet.pyrotech.prototype.campfire;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Replaces athenaeum's TileDataService: subclasses put their client-visible
 * state in saveSynced/loadSynced and call sync() after a server-side change.
 * The whole synced snapshot ships in one vanilla block entity data packet
 * instead of per-field deltas.
 */
public abstract class SyncedBlockEntity extends BlockEntity {

    protected SyncedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected abstract void saveSynced(CompoundTag tag, HolderLookup.Provider registries);

    protected abstract void loadSynced(CompoundTag tag, HolderLookup.Provider registries);

    protected void onSyncedDataUpdate() {
    }

    public final void sync() {
        if (this.level != null && !this.level.isClientSide) {
            this.setChanged();
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.saveSynced(tag, registries);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.loadSynced(tag, registries);
    }

    @Override
    public final CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        this.saveSynced(tag, registries);
        return tag;
    }

    @Override
    public final Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public final void onDataPacket(Connection net, ClientboundBlockEntityDataPacket packet, HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            this.loadSynced(tag, registries);
            this.onSyncedDataUpdate();
        }
    }

    @Override
    public final void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        this.loadSynced(tag, registries);
        this.onSyncedDataUpdate();
    }
}
