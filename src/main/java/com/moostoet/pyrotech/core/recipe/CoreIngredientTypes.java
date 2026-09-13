package com.moostoet.pyrotech.core.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Core's custom crafting ingredients. */
public final class CoreIngredientTypes {

    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
        DeferredRegister.create(NeoForgeRegistries.Keys.INGREDIENT_TYPES, Pyrotech.MOD_ID);

    public static final DeferredHolder<IngredientType<?>, IngredientType<FluidContainerIngredient>> FLUID_CONTAINER =
        INGREDIENT_TYPES.register("fluid_container", () -> new IngredientType<>(FluidContainerIngredient.CODEC));

    private CoreIngredientTypes() {
    }
}
