package com.moostoet.pyrotech.tool.recipe;

import com.moostoet.pyrotech.Pyrotech;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Tool's recipe serializers. The recipe files are datagen's. */
public final class ToolRecipeSerializers {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, Pyrotech.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ToolRepairRecipe>> TOOL_REPAIR =
        RECIPE_SERIALIZERS.register("tool_repair", ToolRepairRecipe.Serializer::new);

    private ToolRecipeSerializers() {
    }
}
