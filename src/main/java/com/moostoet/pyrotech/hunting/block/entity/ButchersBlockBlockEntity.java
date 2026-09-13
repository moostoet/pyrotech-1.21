package com.moostoet.pyrotech.hunting.block.entity;

import com.moostoet.pyrotech.hunting.HuntingBlockEntities;
import com.moostoet.pyrotech.hunting.HuntingBlocks;
import com.moostoet.pyrotech.hunting.HuntingDataMaps;
import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * The butcher's block's one input slot, holding one carcass item, reached from the top. The
 * knife loop works the carcass item's own contents and puts the lighter item back, so the
 * slot's stack is what the renderer draws and what a break drops.
 */
public final class ButchersBlockBlockEntity extends SyncedBlockEntity implements Butchering.Target, ScrollInteractable {

    private static final float EXHAUSTION = 0.75f;

    private final ItemStackHandler input = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(HuntingBlocks.CARCASS_ITEM.get());
        }

        @Override
        protected void onContentsChanged(int slot) {
            ButchersBlockBlockEntity.this.resetProgress();
            ButchersBlockBlockEntity.this.sync();
        }
    };
    private float progress = Butchering.randomProgress();

    public ButchersBlockBlockEntity(BlockPos pos, BlockState state) {
        super(HuntingBlockEntities.BUTCHERS_BLOCK.get(), pos, state);
    }

    public ItemStack carcass() {
        return this.input.getStackInSlot(0);
    }

    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        return hit.getDirection() == Direction.UP && SlotInteraction.insert(this.input, 0, player, held);
    }

    public boolean extract(Player player, BlockHitResult hit) {
        return hit.getDirection() == Direction.UP && SlotInteraction.extract(this.input, 0, player, this.worldPosition);
    }

    @Override
    public void scroll(Player player, BlockHitResult hit, boolean up) {
        if (hit.getDirection() != Direction.UP) {
            return;
        }
        if (up) {
            SlotInteraction.scrollInsert(this.input, 0, player);
        } else {
            SlotInteraction.extractOne(this.input, 0, player, this.worldPosition);
        }
    }

    public void dropContents() {
        if (this.level != null && !this.carcass().isEmpty()) {
            Containers.dropItemStack(this.level, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1,
                this.worldPosition.getZ() + 0.5, this.carcass());
        }
    }

    @Override
    public Level level() {
        return this.level;
    }

    @Override
    public BlockPos pos() {
        return this.worldPosition;
    }

    @Override
    public boolean hasWork() {
        return !this.carcass().isEmpty();
    }

    @Override
    public float exhaustion() {
        return EXHAUSTION;
    }

    @Override
    public boolean atButchersBlock() {
        return true;
    }

    @Override
    public float progress() {
        return this.progress;
    }

    @Override
    public void setProgress(float progress) {
        this.progress = progress;
    }

    @Override
    public void resetProgress() {
        this.progress = Butchering.randomProgress();
    }

    @Override
    public ItemStack extract(ItemStack knife) {
        ItemStack carcass = this.carcass();
        List<ItemStack> contents = new ArrayList<>(carcass.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).stream().toList());
        ItemStack taken = ItemStack.EMPTY;
        for (ItemStack stack : contents) {
            if (!stack.isEmpty()) {
                taken = stack.split(1);
                break;
            }
        }
        if (taken.isEmpty()) {
            return ItemStack.EMPTY;
        }
        carcass.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(contents));
        this.input.setStackInSlot(0, carcass);
        return HuntingDataMaps.transform(taken, knife, this.level == null ? RandomSource.create() : this.level.random);
    }

    @Override
    public boolean isEmpty() {
        ItemStack carcass = this.carcass();
        return carcass.isEmpty()
            || !carcass.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyItems().iterator().hasNext();
    }

    @Override
    public void destroy() {
        this.input.setStackInSlot(0, ItemStack.EMPTY);
    }

    @Override
    public double particleOffsetY() {
        return 1.5;
    }

    @Override
    protected void saveSynced(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("input", this.input.serializeNBT(registries));
    }

    @Override
    protected void loadSynced(CompoundTag tag, HolderLookup.Provider registries) {
        this.input.deserializeNBT(registries, tag.getCompound("input"));
    }
}
