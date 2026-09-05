package com.moostoet.pyrotech.core.loot;

import com.moostoet.pyrotech.Pyrotech;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** The serializers of core's three global loot modifiers. The modifier files are datagen's. */
public final class CoreLootModifiers {

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> SERIALIZERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Pyrotech.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<IronIngotLootModifier>> REPLACE_IRON_INGOTS =
        SERIALIZERS.register("replace_iron_ingots", () -> IronIngotLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<VillageFurnaceLootModifier>> REMOVE_VILLAGE_FURNACE =
        SERIALIZERS.register("remove_village_furnace", () -> VillageFurnaceLootModifier.CODEC);
    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<LeavesStickLootModifier>> STICKS_FROM_LEAVES =
        SERIALIZERS.register("sticks_from_leaves", () -> LeavesStickLootModifier.CODEC);

    private CoreLootModifiers() {
    }
}
