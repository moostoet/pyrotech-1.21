package com.moostoet.pyrotech.storage.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Storage's recipe serializers. The recipe files are datagen's. */
public final class StorageRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TankFlushRecipe>> TANK_FLUSH =
        RECIPE_SERIALIZERS.register("tank_flush", TankFlushRecipe.Serializer::new);

    private StorageRecipeSerializers() {
    }
}
