package com.moostoet.pyrotech.core.item;

import com.moostoet.pyrotech.core.entity.BookItemEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The guide book. Dropped, it becomes a {@link BookItemEntity} that never despawns and
 * cannot be destroyed. Opening it is the Patchouli unit's work, last in the porting
 * order; until then a right click does nothing.
 */
public final class BookItem extends Item {

    public BookItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean hasCustomEntity(ItemStack stack) {
        return true;
    }

    @Override
    public Entity createEntity(Level level, Entity location, ItemStack stack) {
        return BookItemEntity.replacing(level, location, stack);
    }
}
