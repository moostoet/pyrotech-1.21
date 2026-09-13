package com.moostoet.pyrotech.hunting;

import com.moostoet.pyrotech.Pyrotech;
import com.moostoet.pyrotech.hunting.entity.MudEntity;
import com.moostoet.pyrotech.hunting.entity.PyrotechArrowEntity;
import com.moostoet.pyrotech.hunting.entity.SoakingHideItemEntity;
import com.moostoet.pyrotech.hunting.entity.SpearEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Hunting's five entities under their flattened 1.12 ids. The mud takes vanilla slime's
 * size and spawn scale; the projectiles vanilla arrow's. Every tracker is the 1.12 one:
 * five chunks, every tick, except the soaking hide's every four ticks.
 */
public final class HuntingEntities {

    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, Pyrotech.MOD_ID);

    public static final DeferredHolder<EntityType<?>, EntityType<MudEntity>> MUD = ENTITY_TYPES.register("mud", () ->
        EntityType.Builder.of(MudEntity::new, MobCategory.MONSTER)
            .sized(0.52F, 0.52F)
            .eyeHeight(0.325F)
            .spawnDimensionsScale(4.0F)
            .clientTrackingRange(5)
            .updateInterval(1)
            .build(Pyrotech.MOD_ID + ":mud"));

    public static final DeferredHolder<EntityType<?>, EntityType<SpearEntity>> SPEAR = ENTITY_TYPES.register("spear", () ->
        EntityType.Builder.<SpearEntity>of(SpearEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(5)
            .updateInterval(1)
            .build(Pyrotech.MOD_ID + ":spear"));

    public static final DeferredHolder<EntityType<?>, EntityType<PyrotechArrowEntity>> FLINT_ARROW = arrow("flint_arrow");
    public static final DeferredHolder<EntityType<?>, EntityType<PyrotechArrowEntity>> BONE_ARROW = arrow("bone_arrow");

    public static final DeferredHolder<EntityType<?>, EntityType<SoakingHideItemEntity>> HIDE_SCRAPED_ITEM =
        ENTITY_TYPES.register("hide_scraped_item", () ->
            EntityType.Builder.<SoakingHideItemEntity>of(SoakingHideItemEntity::new, MobCategory.MISC)
                .sized(0.25F, 0.25F)
                .eyeHeight(0.2125F)
                .clientTrackingRange(5)
                .updateInterval(4)
                .build(Pyrotech.MOD_ID + ":hide_scraped_item"));

    private HuntingEntities() {
    }

    private static DeferredHolder<EntityType<?>, EntityType<PyrotechArrowEntity>> arrow(String name) {
        return ENTITY_TYPES.register(name, () -> EntityType.Builder.<PyrotechArrowEntity>of(PyrotechArrowEntity::new, MobCategory.MISC)
            .sized(0.5F, 0.5F)
            .eyeHeight(0.13F)
            .clientTrackingRange(5)
            .updateInterval(1)
            .build(Pyrotech.MOD_ID + ":" + name));
    }
}
