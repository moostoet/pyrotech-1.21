package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.block.StashBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** The stash's one large slot, reached from the top. */
public final class StashBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    private final LargeStackHandler handler;

    public StashBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.STASH.get(), pos, state);
        this.handler = new LargeStackHandler(1, ((StashBlock) state.getBlock()).maxStacks()) {
            @Override
            protected void onContentsChanged(int slot) {
                StashBlockEntity.this.sync();
            }
        };
    }

    public LargeStackHandler handler() {
        return this.handler;
    }

    public Direction facing() {
        return this.getBlockState().getValue(StashBlock.FACING);
    }

    /** The slot a hit targets: the one slot, from the top only. */
    public int slotAt(BlockHitResult hit) {
        return hit.getDirection() == Direction.UP ? 0 : -1;
    }

    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        return this.slotAt(hit) == 0 && SlotInteraction.insert(this.handler, 0, player, held);
    }

    public boolean extract(Player player, BlockHitResult hit) {
        return this.slotAt(hit) == 0 && SlotInteraction.extract(this.handler, 0, player, this.worldPosition);
    }

    @Override
    public void scroll(ServerPlayer player, BlockHitResult hit, boolean up) {
        if (this.slotAt(hit) != 0) {
            return;
        }
        if (up) {
            SlotInteraction.scrollInsert(this.handler, 0, player);
        } else {
            SlotInteraction.extractOne(this.handler, 0, player, this.worldPosition);
        }
    }

    public void dropContents() {
        if (this.level != null) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY() + 1, this.worldPosition.getZ(),
                this.handler.getStackInSlot(0));
        }
    }

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("items", this.handler.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.handler.deserializeNBT(registries, tag.getCompound("items"));
    }
}
