package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.entity.BookItemEntity;
import com.moostoet.pyrotech.core.entity.PyroberryCocktailEntity;
import com.moostoet.pyrotech.core.entity.ThrownRockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/**
 * Core's entities: the three thrown rocks and the cocktail, with the 1.12 ids and a
 * five-chunk, every-tick tracker, and the dropped book, tracked every four ticks as 1.12 did.
 */
public final class CoreEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK =
        thrownRock("rock", () -> CoreBlocks.ROCK_STONE.get().asItem());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK_GRASS =
        thrownRock("rock_grass", () -> CoreBlocks.ROCK_GRASS.get().asItem());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK_NETHERRACK =
        thrownRock("rock_netherrack", () -> CoreBlocks.ROCK_NETHERRACK.get().asItem());

    public static final DeferredHolder<EntityType<?>, EntityType<PyroberryCocktailEntity>> PYROBERRY_COCKTAIL =
        ENTITY_TYPES.register("pyroberry_cocktail", () -> EntityType.Builder
            .<PyroberryCocktailEntity>of(PyroberryCocktailEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(5)
            .updateInterval(1)
            .build(Pyrotech.MOD_ID + ":pyroberry_cocktail"));

    // Vanilla's item entity size and eye height over the 1.12 tracker.
    public static final DeferredHolder<EntityType<?>, EntityType<BookItemEntity>> BOOK =
        ENTITY_TYPES.register("book", () -> EntityType.Builder
            .<BookItemEntity>of(BookItemEntity::new, MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .eyeHeight(0.2125F)
            .clientTrackingRange(5)
            .updateInterval(4)
            .build(Pyrotech.MOD_ID + ":book"));

    private CoreEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> thrownRock(String name, Supplier<Item> defaultItem) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder
            .<ThrownRockEntity>of((type, level) -> new ThrownRockEntity(type, level, defaultItem), MobCategory.MISC)
            .sized(0.25F, 0.25F)
            .clientTrackingRange(5)
            .updateInterval(1)
            .build(Pyrotech.MOD_ID + ":" + name));
    }
}
