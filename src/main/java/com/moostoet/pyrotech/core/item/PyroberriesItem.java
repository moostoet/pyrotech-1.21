package com.moostoet.pyrotech.core.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Pyroberries set whoever eats them on fire for five seconds. */
public final class PyroberriesItem extends Item {

    public PyroberriesItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        entity.igniteForSeconds(5);
        return super.finishUsingItem(stack, level, entity);
    }
}
