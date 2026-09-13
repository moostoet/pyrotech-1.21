package com.moostoet.pyrotech.core.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Core's recipe serializers. The recipe files are datagen's. */
public final class CoreRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolDamageShapelessRecipe>> TOOL_DAMAGE_SHAPELESS =
        RECIPE_SERIALIZERS.register("tool_damage_shapeless", ToolDamageShapelessRecipe.Serializer::new);

    private CoreRecipeSerializers() {
    }
}
