package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.HitSlots;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.block.WoodRackBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * The wood rack's nine log slots. Logs settle downward after every change, and by hand an
 * empty slot refuses a log while the slot below it is empty or still has room.
 */
public final class WoodRackBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    public static final int COLUMNS = 3;
    public static final int ROWS = 3;
    /** The opening the slots fill: x 3 to 13 and y 4 to 14 sixteenths. */
    public static final double OPENING_MIN_X = 3 / 16.0;
    public static final double OPENING_MAX_X = 13 / 16.0;
    public static final double OPENING_MIN_Y = 4 / 16.0;
    public static final double OPENING_MAX_Y = 14 / 16.0;

    private final ItemStackHandler handler;
    private boolean settling;

    public WoodRackBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.WOOD_RACK.get(), pos, state);
        this.handler = new ItemStackHandler(COLUMNS * ROWS) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return stack.is(ItemTags.LOGS);
            }

            @Override
            protected void onContentsChanged(int slot) {
                WoodRackBlockEntity.this.onContentsChanged();
            }
        };
    }

    public ItemStackHandler handler() {
        return this.handler;
    }

    public Direction facing() {
        return this.getBlockState().getValue(WoodRackBlock.FACING);
    }

    /** The slot a hit targets, from any side: the column and row of the opening the hit falls in. */
    public int slotAt(BlockHitResult hit) {
        Vec3 local = HitSlots.local(hit, this.worldPosition, this.facing());
        int column = HitSlots.cell(local.x, OPENING_MIN_X, OPENING_MAX_X, COLUMNS);
        int row = HitSlots.cell(local.y, OPENING_MIN_Y, OPENING_MAX_Y, ROWS);
        return row * COLUMNS + column;
    }

    /** The 1.12 validation for a hand: a log, and not floating over a slot that has room. */
    public boolean accepts(int slot, ItemStack stack) {
        if (!stack.is(ItemTags.LOGS)) {
            return false;
        }
        if (slot >= COLUMNS && this.handler.getStackInSlot(slot).isEmpty()) {
            int below = slot - COLUMNS;
            if (this.handler.getStackInSlot(below).isEmpty()
                || this.handler.insertItem(below, stack, true).getCount() < stack.getCount()) {
                return false;
            }
        }
        return true;
    }

    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        int slot = this.slotAt(hit);
        if (!this.accepts(slot, held) || !SlotInteraction.insert(this.handler, slot, player, held)) {
            return false;
        }
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOD_PLACE, SoundSource.BLOCKS, 0.75f,
                (float) (1 + this.level.random.nextGaussian() * 0.4));
        }
        return true;
    }

    public boolean extract(Player player, BlockHitResult hit) {
        return SlotInteraction.extract(this.handler, this.slotAt(hit), player, this.worldPosition);
    }

    @Override
    public void scroll(ServerPlayer player, BlockHitResult hit, boolean up) {
        int slot = this.slotAt(hit);
        if (up) {
            if (this.accepts(slot, player.getMainHandItem())) {
                SlotInteraction.scrollInsert(this.handler, slot, player);
            }
        } else {
            SlotInteraction.extractOne(this.handler, slot, player, this.worldPosition);
        }
    }

    private void onContentsChanged() {
        if (this.settling) {
            return;
        }
        if (this.level != null && !this.level.isClientSide) {
            this.settle();
        }
        this.sync();
    }

    /** The upper rows fall into the rows below them, as far as the stacks allow. */
    private void settle() {
        this.settling = true;
        for (int slot = 0; slot < COLUMNS * (ROWS - 1); slot++) {
            int above = slot + COLUMNS;
            ItemStack falling = this.handler.extractItem(above, this.handler.getSlotLimit(above), false);
            falling = this.handler.insertItem(slot, falling, false);
            this.handler.insertItem(above, falling, false);
        }
        this.settling = false;
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
