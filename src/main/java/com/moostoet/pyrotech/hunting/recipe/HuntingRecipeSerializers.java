package com.moostoet.pyrotech.hunting.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Hunting's recipe serializers: the four rehomed core recipe classes that needed one. The recipe files are datagen's. */
public final class HuntingRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ShearingPeltRecipe>> SHEARING_PELT =
        RECIPE_SERIALIZERS.register("shearing_pelt", ShearingPeltRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LeatherRepairRecipe>> LEATHER_REPAIR =
        RECIPE_SERIALIZERS.register("leather_repair", LeatherRepairRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LeatherDurableUpgradeRecipe>> LEATHER_DURABLE_UPGRADE =
        RECIPE_SERIALIZERS.register("leather_durable_upgrade", LeatherDurableUpgradeRecipe.Serializer::new);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<LeatherFireProtectionRecipe>> LEATHER_FIRE_PROTECTION =
        RECIPE_SERIALIZERS.register("leather_fire_protection", LeatherFireProtectionRecipe.Serializer::new);

    private HuntingRecipeSerializers() {
    }
}
