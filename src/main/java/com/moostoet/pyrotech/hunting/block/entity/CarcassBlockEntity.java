package com.moostoet.pyrotech.hunting.block.entity;

import com.moostoet.pyrotech.hunting.HuntingBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * The carcass's contents: the captured drops, one stack per kind, worked out one item at a
 * time from the first stack that has any. The progress is not saved, as 1.12 did not save it.
 */
public final class CarcassBlockEntity extends BlockEntity implements Butchering.Target {

    private static final float EXHAUSTION = 1.5f;

    private List<ItemStack> items = new ArrayList<>();
    private float progress = Butchering.randomProgress();

    public CarcassBlockEntity(BlockPos pos, BlockState state) {
        super(HuntingBlockEntities.CARCASS.get(), pos, state);
    }

    public List<ItemStack> items() {
        return List.copyOf(this.items);
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
        return true;
    }

    @Override
    public float exhaustion() {
        return EXHAUSTION;
    }

    @Override
    public boolean atButchersBlock() {
        return false;
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
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                ItemStack taken = stack.split(1);
                this.items.removeIf(ItemStack::isEmpty);
                this.setChanged();
                return taken;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean isEmpty() {
        return this.items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public void destroy() {
        if (this.level != null) {
            this.level.destroyBlock(this.worldPosition, false);
        }
    }

    @Override
    public double particleOffsetY() {
        return 1;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag list = new ListTag();
        for (ItemStack stack : this.items) {
            if (!stack.isEmpty()) {
                list.add(stack.save(registries));
            }
        }
        tag.put("Items", list);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = new ArrayList<>();
        for (Tag entry : tag.getList("Items", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entry).ifPresent(this.items::add);
        }
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(this.items));
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        this.items = new ArrayList<>(input.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).nonEmptyStream().toList());
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("Items");
    }
}
