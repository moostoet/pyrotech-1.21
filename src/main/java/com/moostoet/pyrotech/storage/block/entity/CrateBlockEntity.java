package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.library.inventory.LargeStackHandler;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.block.CrateBlock;
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
import net.minecraft.world.phys.Vec3;

/** The crate's nine slots, a three by three grid seen from the top. */
public final class CrateBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int COLUMNS = 3;
    public static final int ROWS = 3;

    private final LargeStackHandler handler;

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.CRATE.get(), pos, state);
        this.handler = new LargeStackHandler(COLUMNS * ROWS, ((CrateBlock) state.getBlock()).maxStacks()) {
            @Override
            protected void onContentsChanged(int slot) {
                CrateBlockEntity.this.sync();
            }
        };
    }

    public LargeStackHandler handler() {
        return this.handler;
    }

    public Direction facing() {
        return this.getBlockState().getValue(CrateBlock.FACING);
    }

    /** The slot a hit targets: the grid cell on the top, or -1 from any other side. */
    public int slotAt(BlockHitResult hit) {
        if (hit.getDirection() != Direction.UP) {
            return -1;
        }
        Vec3 local = HitSlots.local(hit, this.worldPosition, this.facing());
        return HitSlots.cell(local.z, 0, 1, ROWS) * COLUMNS + HitSlots.cell(local.x, 0, 1, COLUMNS);
    }

    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        return slot >= 0 && SlotInteraction.insert(this.handler, slot, player, held);
    }

    public boolean extract(Player player, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        return slot >= 0 && SlotInteraction.extract(this.handler, slot, player, this.worldPosition);
    }

    @Override
    public void scroll(ServerPlayer player, BlockHitResult hit, boolean up) {
        int slot = this.slotAt(hit);
        if (slot < 0) {
            return;
        }
        if (up) {
            SlotInteraction.scrollInsert(this.handler, slot, player);
        } else {
            SlotInteraction.extractOne(this.handler, slot, player, this.worldPosition);
        }
    }

    public void dropContents() {
        if (this.level == null) {
            return;
        }
        for (int slot = 0; slot < this.handler.getSlots(); slot++) {
            Containers.dropItemStack(this.level, this.worldPosition.getX(), this.worldPosition.getY() + 1, this.worldPosition.getZ(),
                this.handler.getStackInSlot(slot));
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
