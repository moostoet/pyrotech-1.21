package com.moostoet.pyrotech.storage.block.entity;

import com.moostoet.pyrotech.library.block.SyncedBlockEntity;
import com.moostoet.pyrotech.library.interaction.ScrollInteractable;
import com.moostoet.pyrotech.library.interaction.SlotInteraction;
import com.moostoet.pyrotech.storage.StorageBlockEntities;
import com.moostoet.pyrotech.storage.StorageComponents;
import com.moostoet.pyrotech.storage.block.BagBlock;
import com.moostoet.pyrotech.storage.item.BagContents;
import com.moostoet.pyrotech.storage.item.BagStackHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A placed rock bag: its items in a large handler, fed from the top while open and emptied
 * from the last filled slot. Its contents ride the {@code bag_contents} component when it
 * is picked up, and the open state is the block's {@code type}.
 */
public final class BagBlockEntity extends SyncedBlockEntity implements ScrollInteractable {

    private final BagStackHandler handler;

    public BagBlockEntity(BlockPos pos, BlockState state) {
        super(StorageBlockEntities.BAG.get(), pos, state);
        BagBlock block = (BagBlock) state.getBlock();
        this.handler = new BagStackHandler(block.capacity(), stack -> stack.is(block.allowed())) {
            @Override
            protected void onContentsChanged(int slot) {
                BagBlockEntity.this.sync();
            }
        };
    }

    public BagStackHandler handler() {
        return this.handler;
    }

    public BagBlock block() {
        return (BagBlock) this.getBlockState().getBlock();
    }

    public Direction facing() {
        return this.getBlockState().getValue(BagBlock.FACING);
    }

    public boolean isOpen() {
        return BagBlock.isOpen(this.getBlockState());
    }

    public void toggleOpen() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.setBlock(this.worldPosition, this.getBlockState().cycle(BagBlock.TYPE), Block.UPDATE_ALL);
        }
    }

    /** Whether a hit is on the input: the top of an open bag. */
    public boolean isInput(BlockHitResult hit) {
        return this.isOpen() && hit.getDirection() == Direction.UP;
    }

    public boolean accepts(ItemStack stack) {
        return this.handler.isItemValid(0, stack) && this.handler.remainingCapacity() > 0;
    }

    public boolean insert(Player player, ItemStack held, BlockHitResult hit) {
        if (!this.isInput(hit) || !SlotInteraction.insert(this.handler, 0, player, held)) {
            return false;
        }
        if (this.level != null && !this.level.isClientSide) {
            this.level.playSound(null, this.worldPosition, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.5f,
                (float) (1 + this.level.random.nextGaussian() * 0.4));
        }
        return true;
    }

    public boolean extract(Player player, BlockHitResult hit) {
        return this.isInput(hit) && !this.handler.isEmpty() && SlotInteraction.extract(this.handler, 0, player, this.worldPosition);
    }

    @Override
    public void scroll(ServerPlayer player, BlockHitResult hit, boolean up) {
        if (!this.isInput(hit)) {
            return;
        }
        if (up) {
            SlotInteraction.scrollInsert(this.handler, 0, player);
        } else {
            SlotInteraction.extractOne(this.handler, 0, player, this.worldPosition);
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        input.getOrDefault(StorageComponents.BAG_CONTENTS, BagContents.EMPTY).copyInto(this.handler);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.handler.isEmpty()) {
            components.set(StorageComponents.BAG_CONTENTS, BagContents.of(this.handler));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("items");
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
