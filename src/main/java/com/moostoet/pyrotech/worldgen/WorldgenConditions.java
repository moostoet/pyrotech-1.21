package com.moostoet.pyrotech.worldgen;

import com.moostoet.pyrotech.Pyrotech;
import com.mojang.serialization.MapCodec;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Worldgen's datapack condition codecs. */
public final class WorldgenConditions {

    public static final DeferredRegister<MapCodec<? extends ICondition>> CONDITION_CODECS =
        DeferredRegister.create(NeoForgeRegistries.Keys.CONDITION_CODECS, Pyrotech.MOD_ID);

    public static final DeferredHolder<MapCodec<? extends ICondition>, MapCodec<WorldgenToggleCondition>> WORLDGEN_TOGGLE =
        CONDITION_CODECS.register("worldgen_toggle", () -> WorldgenToggleCondition.CODEC);

    private WorldgenConditions() {
    }
}
