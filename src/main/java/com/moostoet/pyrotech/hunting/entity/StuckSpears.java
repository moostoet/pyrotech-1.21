package com.moostoet.pyrotech.hunting.entity;

import com.mojang.serialization.Codec;
import com.moostoet.pyrotech.hunting.HuntingAttachments;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** The spear stacks stuck in one entity. The client draws them; the server drops them on death. */
public record StuckSpears(List<ItemStack> spears) {

    public static final StuckSpears EMPTY = new StuckSpears(List.of());

    public static final Codec<StuckSpears> CODEC = ItemStack.CODEC.listOf().xmap(StuckSpears::new, StuckSpears::spears);
    public static final StreamCodec<RegistryFriendlyByteBuf, StuckSpears> STREAM_CODEC =
        ItemStack.LIST_STREAM_CODEC.map(StuckSpears::new, StuckSpears::spears);

    public boolean isEmpty() {
        return this.spears.isEmpty();
    }

    public StuckSpears with(ItemStack spear) {
        List<ItemStack> all = new ArrayList<>(this.spears);
        all.add(spear.copy());
        return new StuckSpears(List.copyOf(all));
    }

    public static StuckSpears of(LivingEntity entity) {
        return entity.getExistingData(HuntingAttachments.STUCK_SPEARS).orElse(EMPTY);
    }

    public static void add(LivingEntity entity, ItemStack spear) {
        entity.setData(HuntingAttachments.STUCK_SPEARS, of(entity).with(spear));
    }
}
