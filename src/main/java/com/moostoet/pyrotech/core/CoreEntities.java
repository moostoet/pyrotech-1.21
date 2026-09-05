package com.moostoet.pyrotech.core;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.core.entity.ThrownRockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Core's entities: the three thrown rocks, with the 1.12 ids and a five-chunk, every-tick tracker. */
public final class CoreEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK =
        thrownRock("rock", () -> CoreBlocks.ROCK_STONE.get().asItem());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK_GRASS =
        thrownRock("rock_grass", () -> CoreBlocks.ROCK_GRASS.get().asItem());
    public static final DeferredHolder<EntityType<?>, EntityType<ThrownRockEntity>> ROCK_NETHERRACK =
        thrownRock("rock_netherrack", () -> CoreBlocks.ROCK_NETHERRACK.get().asItem());

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
