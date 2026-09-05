package com.moostoet.pyrotech.core.entity;

import com.moostoet.pyrotech.core.CoreEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The dropped guide book: an item entity with no lifetime, invulnerable, that smoulders
 * with flame particles and the odd crackle. Fire immunity comes from the item's
 * fire-resistant component.
 */
public final class BookItemEntity extends ItemEntity {

    public BookItemEntity(EntityType<? extends BookItemEntity> type, Level level) {
        super(type, level);
        this.setUnlimitedLifetime();
        this.setInvulnerable(true);
    }

    /** The entity that takes the place of a plain item entity carrying the book. */
    public static BookItemEntity replacing(Level level, Entity item, ItemStack stack) {
        BookItemEntity book = new BookItemEntity(CoreEntities.BOOK.get(), level);
        book.setPos(item.position());
        book.setDeltaMovement(item.getDeltaMovement());
        book.setItem(stack);
        book.setPickUpDelay(40);
        return book;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide || this.level().getGameTime() % 5 != 0) {
            return;
        }
        if (this.random.nextFloat() < 0.1F) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 1, 1, false);
        }
        if (this.random.nextFloat() < 0.5F) {
            double y = this.getY() + 6.0 / 16.0 + this.random.nextDouble() * 2.0 / 16.0;
            this.level().addParticle(ParticleTypes.FLAME,
                this.getX() + (this.random.nextDouble() * 2 - 1) * 0.1,
                y + (this.random.nextDouble() * 2 - 1) * 0.1,
                this.getZ() + (this.random.nextDouble() * 2 - 1) * 0.1,
                0, 0, 0);
        }
    }
}
