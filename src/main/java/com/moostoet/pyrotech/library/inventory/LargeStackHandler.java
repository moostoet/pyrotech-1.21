package com.moostoet.pyrotech.library.inventory;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.function.Function;

/**
 * An item handler whose slots hold more than a vanilla stack (storage sign-off, item 1).
 * A slot takes {@code maxStacks} stacks of its item, and the hopper hook's slot limit says
 * so too. {@code ItemStack.CODEC} refuses counts above 99, so each slot saves as the item
 * plus its own count.
 */
public class LargeStackHandler extends ItemStackHandler {

    /** A stack of any count: the single-item codec plus the count. */
    public static final Codec<ItemStack> STACK_CODEC = RecordCodecBuilder.create(instance -> instance.group(
        ItemStack.SINGLE_ITEM_CODEC.fieldOf("item").forGetter(Function.identity()),
        ExtraCodecs.POSITIVE_INT.fieldOf("count").forGetter(ItemStack::getCount)
    ).apply(instance, ItemStack::copyWithCount));

    private final int maxStacks;

    public LargeStackHandler(int size, int maxStacks) {
        super(size);
        this.maxStacks = maxStacks;
    }

    @Override
    public int getSlotLimit(int slot) {
        return Item.DEFAULT_MAX_STACK_SIZE * this.maxStacks;
    }

    @Override
    protected int getStackLimit(int slot, ItemStack stack) {
        return stack.getMaxStackSize() * this.maxStacks;
    }

    public int totalCount() {
        int total = 0;
        for (ItemStack stack : this.stacks) {
            total += stack.getCount();
        }
        return total;
    }

    public boolean isEmpty() {
        for (ItemStack stack : this.stacks) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        ListTag items = new ListTag();
        for (int slot = 0; slot < this.stacks.size(); slot++) {
            ItemStack stack = this.stacks.get(slot);
            if (!stack.isEmpty()) {
                CompoundTag tag = (CompoundTag) STACK_CODEC.encodeStart(ops, stack).getOrThrow();
                tag.putInt("Slot", slot);
                items.add(tag);
            }
        }
        CompoundTag nbt = new CompoundTag();
        nbt.put("Items", items);
        nbt.putInt("Size", this.stacks.size());
        return nbt;
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        RegistryOps<Tag> ops = provider.createSerializationContext(NbtOps.INSTANCE);
        this.setSize(nbt.contains("Size", Tag.TAG_INT) ? nbt.getInt("Size") : this.stacks.size());
        ListTag items = nbt.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < items.size(); i++) {
            CompoundTag tag = items.getCompound(i);
            int slot = tag.getInt("Slot");
            if (slot >= 0 && slot < this.stacks.size()) {
                STACK_CODEC.parse(ops, tag)
                    .resultOrPartial(error -> Pyrotech.LOGGER.warn("Failed to load a large stack: {}", error))
                    .ifPresent(stack -> this.stacks.set(slot, stack));
            }
        }
        this.onLoad();
    }
}
